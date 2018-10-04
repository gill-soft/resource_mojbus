package com.gillsoft.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gillsoft.cache.CacheHandler;
import com.gillsoft.cache.IOCacheException;
import com.gillsoft.cache.RedisMemoryCache;
import com.gillsoft.logging.SimpleRequestResponseLoggingInterceptor;
import com.gillsoft.model.Lang;
import com.gillsoft.model.Locality;
import com.gillsoft.model.Response;
import com.gillsoft.model.ServiceItem;
import com.gillsoft.util.RestTemplateUtil;
import com.gillsoft.util.StringUtil;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.tool.xml.Pipeline;
import com.itextpdf.tool.xml.XMLWorker;
import com.itextpdf.tool.xml.XMLWorkerHelper;
import com.itextpdf.tool.xml.css.StyleAttrCSSResolver;
import com.itextpdf.tool.xml.html.Tags;
import com.itextpdf.tool.xml.parser.XMLParser;
import com.itextpdf.tool.xml.pipeline.css.CSSResolver;
import com.itextpdf.tool.xml.pipeline.css.CssResolverPipeline;
import com.itextpdf.tool.xml.pipeline.end.PdfWriterPipeline;
import com.itextpdf.tool.xml.pipeline.html.HtmlPipeline;
import com.itextpdf.tool.xml.pipeline.html.HtmlPipelineContext;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class RestClient {

	public static final String STATIONS_CACHE_KEY = "mojbus.stations.";
	public static final String TRIPS_CACHE_KEY = "mojbus.trips.";
	public static final String TICKET_CACHE_KEY = "mojbus.ticket.";

	private static final String SEARCH = "search";
	private static final String RESERVE = "reserve";
	private static final String CONFIRM = "confirm";
	private static final String REFUND = "refund";
	private static final String TOWNS = "towns";

	private static HttpHeaders headers = new HttpHeaders();
	private static ObjectMapper objectMapper;

    @Autowired
    @Qualifier("RedisMemoryCache")
	private CacheHandler cache;

    static {
		headers.add("Authorization", "");
    	headers.add("Content-Type", "application/json; charset=utf-8");
    	objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

	private RestTemplate template;
	// для запросов поиска с меньшим таймаутом
	private RestTemplate searchTemplate;

	public RestClient() {
		template = createNewPoolingTemplate(Config.getRequestTimeout());
		searchTemplate = createNewPoolingTemplate(Config.getSearchRequestTimeout());
	}

	public RestTemplate createNewPoolingTemplate(int requestTimeout) {
		HttpComponentsClientHttpRequestFactory factory = (HttpComponentsClientHttpRequestFactory) RestTemplateUtil
				.createPoolingFactory(Config.getUrl(), 300, requestTimeout);
		factory.setReadTimeout(Config.getReadTimeout());
		RestTemplate restTemplate = new RestTemplate(new BufferingClientHttpRequestFactory(factory));
		restTemplate.setInterceptors(Collections.singletonList(new SimpleRequestResponseLoggingInterceptor() {
			
			@Override
			public ClientHttpResponse execute(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
					throws IOException {
				ClientHttpResponse clientHttpResponse = super.execute(request, body, execution);
				List<String> headersContentType = clientHttpResponse.getHeaders().get("Content-Type");
				Iterator<String> headersContentTypeIterator = headersContentType.iterator();
				boolean isJson = false;
				while (headersContentTypeIterator.hasNext()) {
					String contentType = headersContentTypeIterator.next();
					if (contentType.startsWith("text/")) {
						headersContentTypeIterator.remove();
					}
					if (!isJson && contentType.contains("json")) {
						isJson = true;
					}
				}
				if (!isJson) {
					headersContentType.add("application/json");
				}
				return clientHttpResponse;
			}
		}));
		return restTemplate;
	}

	/****************** STATIONS ********************/
	@SuppressWarnings("unchecked")
	public SimpleEntry<List<Locality>, Map<String, List<String>>> getCachedStations() throws IOCacheException {
		Map<String, Object> params = new HashMap<>();
		params.put(RedisMemoryCache.OBJECT_NAME, RestClient.STATIONS_CACHE_KEY);
		params.put(RedisMemoryCache.IGNORE_AGE, true);
		params.put(RedisMemoryCache.UPDATE_DELAY, Config.getCacheStationsUpdateDelay());
		params.put(RedisMemoryCache.UPDATE_TASK, new UpdateStationsTask());
		return (SimpleEntry<List<Locality>, Map<String, List<String>>>) cache.read(params);
	}

	public SimpleEntry<List<Locality>, Map<String, List<String>>> getAllStations() throws ResponseError {
		Map<String, Locality> localities = new HashMap<>();
		Map<String, List<String>> fromTo = new HashMap<>();
		try {
			Response townListResponse = getResult(template, getTownsMethod(null), HttpMethod.POST);
			if (townListResponse != null && townListResponse.getTowns() != null
					&& !townListResponse.getTowns().isEmpty()) {
				townListResponse.getTowns().stream().forEach(c -> {
					Locality locality = new Locality();
					locality.setId(StringUtil.md5(c));
					locality.setName(Lang.EN, c);
					localities.put(locality.getId(), locality);
					try {
						Response townToListResponse = getResult(template, getTownsMethod(c), HttpMethod.POST);
						if (townToListResponse != null && townToListResponse.getTowns() != null
								&& !townToListResponse.getTowns().isEmpty()) {
							fromTo.put(locality.getId(), Arrays.asList(townToListResponse.getTowns().stream()
									.map(m -> StringUtil.md5(m)).toArray(String[]::new)));
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new SimpleEntry<>(new ArrayList<>(localities.values()), fromTo);
	}
	
	private URI getTownsMethod(String townFrom) {
		if (townFrom != null && !townFrom.isEmpty()) {
			return UriComponentsBuilder.fromUriString(Config.getUrl() + TOWNS).queryParam("townFrom", townFrom).build().toUri();
		} else {
			return UriComponentsBuilder.fromUriString(Config.getUrl() + TOWNS).build().toUri();
		}
	}

	/****************** TRIPS ********************/
	public TripPackage getCachedTrips(String from, String to, Date dispatch) throws ResponseError {
		URI uri = UriComponentsBuilder.fromUriString(Config.getUrl() + SEARCH).queryParam("departureDate", StringUtil.dateFormat.format(dispatch))
				.queryParam("startTown", from)
				.queryParam("finishTown", to).build().toUri();
		Map<String, Object> params = new HashMap<>();
		params.put(RedisMemoryCache.OBJECT_NAME, RestClient.TRIPS_CACHE_KEY + uri.getQuery());
		params.put(RedisMemoryCache.UPDATE_TASK, new UpdateTripsTask(uri));
		try {
			return (TripPackage) cache.read(params);
		} catch (IOCacheException e) {
			e.printStackTrace();
			// ставим пометку, что кэш еще формируется
			TripPackage tripPackage = new TripPackage();
			tripPackage.setContinueSearch(true);
			return tripPackage;
		} catch (Exception e) {
			throw new ResponseError(e.getMessage());
		}
	}

	public TripPackage getTrips(URI key) throws ResponseError {
		Response response = getResult(searchTemplate, key, HttpMethod.POST);
		TripPackage tripPackage = new TripPackage();
		tripPackage.setCourseList(response.getCourses());
		return tripPackage;
	}

	/****************** BILL/PAY/CANCEL ********************/
	public Response reserve(Map.Entry<String, List<ServiceItem>> entry) throws ResponseError {
		TripIdModel trip = (TripIdModel) new TripIdModel().create(entry.getKey());
		return getResult(template, getReserveUri(entry, trip), HttpMethod.POST);
	}

	private URI getReserveUri(Map.Entry<String, List<ServiceItem>> entry, TripIdModel trip) throws ResponseError {
		ServiceItem customer = null;
		Optional<ServiceItem> optional = entry.getValue().stream()
				.filter(f -> f.getCustomer() != null && f.getCustomer().getName() != null
						&& f.getCustomer().getSurname() != null && f.getCustomer().getPhone() != null
						&& f.getCustomer().getEmail() != null)
				.findFirst();
		if (optional.isPresent()) {
			customer = optional.get();
		} else {
			optional = entry.getValue().stream()
					.filter(f -> f.getCustomer() != null && f.getCustomer().getSurname() != null).findFirst();
			if (optional.isPresent()) {
				customer = optional.get();
			}
		}
		if (customer == null) {
			throw new ResponseError("Can't find valid customer (empty parameters) for " + trip.getIdCourse());
		}
		return UriComponentsBuilder.fromUriString(Config.getUrl() + RESERVE).queryParam("idCourse", trip.getIdCourse())
				.queryParam("startStopNr", trip.getStartStopNr()).queryParam("finishStopNr", trip.getFinishStopNr())
				.queryParam("seatsQtyNormal", entry.getValue().size()).queryParam("lastName", customer.getCustomer().getSurname())
				.queryParam("firstName", customer.getCustomer().getName()).queryParam("phone", customer.getCustomer().getPhone())
				.queryParam("email", customer.getCustomer().getEmail()).build().toUri();
	}

	public Response confirm(String idReservation) throws ResponseError {
		return confirm(idReservation, true);
	}

	public Response cancel(String idReservation) throws ResponseError {
		return confirm(idReservation, false);
	}

	private Response confirm(String idReservation, boolean confirm) throws ResponseError {
		return getResult(template, getConfirmUri(idReservation, confirm), HttpMethod.POST);
	}

	private URI getConfirmUri(String idReservation, boolean confirm) {
		return UriComponentsBuilder.fromUriString(Config.getUrl() + CONFIRM).queryParam("idReservation", idReservation)
				.queryParam("status", confirm ? CONFIRM : "cancel").build().toUri();
	}

	public Response refundPrepare(String idReservation, int seatsRefundQtyNormal) throws ResponseError {
		return getResult(template, getRefundUri(idReservation, seatsRefundQtyNormal, false), HttpMethod.POST);
	}

	public Response refund(String idReservation, int seatsRefundQtyNormal) throws ResponseError {
		return getResult(template, getRefundUri(idReservation, seatsRefundQtyNormal, true), HttpMethod.POST);
	}

	private URI getRefundUri(String idReservation, int seatsRefundQtyNormal, boolean saveRefund) {
		return UriComponentsBuilder.fromUriString(Config.getUrl() + REFUND).queryParam("idReservation", idReservation)
				.queryParam("seatsRefundQtyNormal", seatsRefundQtyNormal)
				.queryParam("saveRefund", String.valueOf(saveRefund)).build().toUri();
	}

	/****************** TICKET ********************/
	public void saveTicketUrl(String idReservation, String ticketUrl, long timeToLive) {
		Map<String, Object> params = new HashMap<>();
		params.put(RedisMemoryCache.OBJECT_NAME, RestClient.TICKET_CACHE_KEY + idReservation);
		params.put(RedisMemoryCache.TIME_TO_LIVE, timeToLive);
		try {
			cache.write(ticketUrl, params);
		} catch (IOCacheException e) {
			e.printStackTrace();
		}
	}

	public String getTicketUrl(String idReservation) throws IOCacheException {
		Map<String, Object> params = new HashMap<>();
		params.put(RedisMemoryCache.OBJECT_NAME, RestClient.TICKET_CACHE_KEY + idReservation);
		return (String) cache.read(params);
	}

	public String getTicketPdf(String ticketUrl) throws IOException {
		String htmlTicket = null;
		try {
			htmlTicket = new String(template.getForObject(ticketUrl, byte[].class), "UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
			throw new IOException(e.getMessage(), e);
		}
		ByteArrayOutputStream baos = null;
		try {
			// удаляем теги мета
			htmlTicket = htmlTicket.replaceAll("<meta(.+?)>", "");
			// загружаем css
			CSSResolver cssResolver = new StyleAttrCSSResolver();
			String host = ticketUrl.replace(UriComponentsBuilder.fromUriString(ticketUrl).build().getPath(), "");
			// формируем полный урл css
			Pattern pattern = Pattern.compile("href=\".+\\.css\"");
			Matcher m = pattern.matcher(htmlTicket);
			while (m.find()) {
				String cssDir = m.group();
				String cssLink = host + cssDir.substring(6, cssDir.length() - 1);
				cssResolver.addCss(
						XMLWorkerHelper.getCSS(new ByteArrayInputStream(template.getForObject(cssLink, byte[].class))));
				htmlTicket = htmlTicket.replaceFirst(cssDir, cssLink);
			}
			// корректируем картинки
			Pattern patternImg = Pattern.compile("src=\"(.+?)\"");
			Matcher mImg = patternImg.matcher(htmlTicket);
			while (mImg.find()) {
				String image = mImg.group();
				image = image.substring(5, image.length() - 1);
				String newImage = host + image;
				htmlTicket = htmlTicket.replaceFirst(image, newImage);
			}
			// исправление бага ресурса
			htmlTicket = htmlTicket.replaceAll("</p>\\s+</p>", "</p>");
			// создаем pdf файл
			com.itextpdf.text.Document doc = new com.itextpdf.text.Document(PageSize.A4, 20, 20, 20, 20);
			baos = new ByteArrayOutputStream();
			PdfWriter writer = PdfWriter.getInstance(doc, baos);
			doc.open();
			// контекст
			HtmlPipelineContext htmlContext = new HtmlPipelineContext(null);
			htmlContext.setTagFactory(Tags.getHtmlTagProcessorFactory());
			// css
			Pipeline<?> pipeline = new CssResolverPipeline(cssResolver,
					new HtmlPipeline(htmlContext, new PdfWriterPipeline(doc, writer)));
			// конвертируем html в pdf
			XMLWorker worker = new XMLWorker(pipeline, true);
			new XMLParser(worker).parse(new StringReader(htmlTicket));
			doc.close();
			worker.close();
			writer.close();
			byte[] pdf = baos.toByteArray();
			return StringUtil.toBase64(pdf);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (baos != null) {
				try {
					baos.flush();
					baos.close();
				} catch (IOException e) {
				}
			}
		}
		return null;
	}

	/*************************************************/
	private Response getResult(RestTemplate template, URI method, HttpMethod httpMethod) throws ResponseError {
		boolean addHeader = false;
		Map<String, String> request = null;
		if (method.getQuery() != null) {
			UriComponents uriComponents = UriComponentsBuilder.fromUri(method).build();
			request = addAuthHeader(uriComponents.getQueryParams());
			addHeader = request != null && !request.isEmpty();
		}
		RequestEntity<Map<String, String>> requestEntity = new RequestEntity<>(
				addHeader ? request : null, addHeader ? headers : null, httpMethod, method);
		ResponseEntity<Response> response = template.exchange(requestEntity, Response.getTypeReference());
		if (response.getBody() == null || response.getBody().getErrorMessage() != null
				|| response.getBody().getErrorNo() != null) {
			throw new ResponseError(response.getBody() == null ? "Empty response"
					: (String.valueOf(response.getBody().getErrorNo()) + ':'
							+ String.valueOf(response.getBody().getErrorMessage())));
		}
		return response.getBody();
	}

	private Map<String, String> addAuthHeader(MultiValueMap<String, String> params) {
		if (params != null && params.size() != 0) {
			try {
				String json = objectMapper.writeValueAsString(params.toSingleValueMap());
				headers.set("Authorization", "Basic " + StringUtil.toBase64(
						Config.getLogin() + ":" + StringUtil.md5(Config.getLogin() + json + Config.getKey())));
				return params.toSingleValueMap();
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public CacheHandler getCache() {
		return cache;
	}

	public static RestClientException createUnavailableMethod() {
		return new RestClientException("Method is unavailable");
	}

}
