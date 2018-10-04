package com.gillsoft;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RestController;

import com.gillsoft.abstract_rest_service.SimpleAbstractTripSearchService;
import com.gillsoft.cache.CacheHandler;
import com.gillsoft.client.ResponseError;
import com.gillsoft.client.RestClient;
import com.gillsoft.client.TripIdModel;
import com.gillsoft.client.TripPackage;
import com.gillsoft.model.Currency;
import com.gillsoft.model.Document;
import com.gillsoft.model.Lang;
import com.gillsoft.model.Locality;
import com.gillsoft.model.Organisation;
import com.gillsoft.model.Price;
import com.gillsoft.model.RequiredField;
import com.gillsoft.model.RestError;
import com.gillsoft.model.ReturnCondition;
import com.gillsoft.model.Route;
import com.gillsoft.model.Seat;
import com.gillsoft.model.SeatsScheme;
import com.gillsoft.model.Segment;
import com.gillsoft.model.Tariff;
import com.gillsoft.model.Trip;
import com.gillsoft.model.TripContainer;
import com.gillsoft.model.Vehicle;
import com.gillsoft.model.request.TripSearchRequest;
import com.gillsoft.model.response.TripSearchResponse;
import com.gillsoft.util.StringUtil;

@RestController
public class SearchServiceController extends SimpleAbstractTripSearchService<TripPackage> {
	
	@Autowired
	private RestClient client;
	
	@Autowired
	@Qualifier("MemoryCacheHandler")
	private CacheHandler cache;

	@Override
	public List<ReturnCondition> getConditionsResponse(String arg0, String arg1) {
		throw RestClient.createUnavailableMethod();
	}

	@Override
	public List<Document> getDocumentsResponse(String arg0) {
		throw RestClient.createUnavailableMethod();
	}

	@Override
	public List<Tariff> getTariffsResponse(String arg0) {
		throw RestClient.createUnavailableMethod();
	}

	@Override
	public List<RequiredField> getRequiredFieldsResponse(String arg0) {
		throw RestClient.createUnavailableMethod();
	}

	@Override
	public Route getRouteResponse(String arg0) {
		throw RestClient.createUnavailableMethod();
	}

	@Override
	public SeatsScheme getSeatsSchemeResponse(String arg0) {
		throw RestClient.createUnavailableMethod();
	}
	
	@Override
	public List<Seat> updateSeatsResponse(String arg0, List<Seat> arg1) {
		throw RestClient.createUnavailableMethod();
	}
	
	@Override
	public List<Seat> getSeatsResponse(String tripId) {
		throw RestClient.createUnavailableMethod();
	}

	@Override
	public TripSearchResponse initSearchResponse(TripSearchRequest request) {
		return simpleInitSearchResponse(cache, request);
	}

	@Override
	public void addInitSearchCallables(List<Callable<TripPackage>> callables, String[] pair, Date date) {
		callables.add(() -> {
			try {
				validateSearchParams(pair, date);
				TripPackage tripPackage = client.getCachedTrips(
						LocalityServiceController.getLocality(pair[0]).getName().get(Lang.EN),
						LocalityServiceController.getLocality(pair[1]).getName().get(Lang.EN), date);
				if (tripPackage == null) {
					throw new ResponseError("Empty result");
				}
				tripPackage.setRequest(TripSearchRequest.createRequest(pair, date));
				return tripPackage;
			} catch (ResponseError e) {
				TripPackage tripPackage = new TripPackage();
				tripPackage.setError(e);
				tripPackage.setRequest(TripSearchRequest.createRequest(pair, date));
				return tripPackage;
			} catch (Exception e) {
				return null;
			}
		});
	}

	private static void validateSearchParams(String[] pair, Date date) throws ResponseError {
		if (date == null
				|| date.getTime() < DateUtils.truncate(new Date(), Calendar.DATE).getTime()) {
			throw new ResponseError("Invalid parameter \"date\"");
		}
		if (pair == null || pair.length < 2 || LocalityServiceController.getLocality(pair[0]) == null
				|| LocalityServiceController.getLocality(pair[1]) == null) {
			throw new ResponseError("Invalid parameter \"pair\"");
		}
	}

	@Override
	public TripSearchResponse getSearchResultResponse(String searchId) {
		return simpleGetSearchResponse(cache, searchId);
	}

	@Override
	public void addNextGetSearchCallablesAndResult(List<Callable<TripPackage>> callables, Map<String, Vehicle> vehicles,
			Map<String, Locality> localities, Map<String, Organisation> organisations, Map<String, Segment> segments,
			List<TripContainer> containers, TripPackage tripPackage) {
		if (!tripPackage.isContinueSearch()) {
			addResult(vehicles, localities, segments, containers, tripPackage);
		} else if (tripPackage.getRequest() != null) {
			addInitSearchCallables(callables, tripPackage.getRequest().getLocalityPairs().get(0),
					tripPackage.getRequest().getDates().get(0));
		}
	}

	private void addResult(Map<String, Vehicle> vehicles, Map<String, Locality> localities,
			Map<String, Segment> segments, List<TripContainer> containers, TripPackage tripPackage) {
		TripContainer container = new TripContainer();
		container.setRequest(tripPackage.getRequest());
		if (tripPackage != null
				&& tripPackage.getCourseList() != null) {
			List<Trip> trips = new ArrayList<>();
			for (com.gillsoft.model.Course course : tripPackage.getCourseList()) {
				Trip tmpTrip = new Trip();
				tmpTrip.setId(new TripIdModel(course.getIdCourse(), course.getStartStopNr(), course.getFinishStopNr(),
						course.getStartDateTime(), course.getFinishDateTime()).asString());
				trips.add(tmpTrip);
				String segmentId = tmpTrip.getId();
				Segment segment = segments.get(segmentId);
				if (segment == null) {
					segment = new Segment();
					segment.setId(segmentId);
					try {
						segment.setDepartureDate(course.getStartDateTime());
						segment.setArrivalDate(course.getFinishDateTime());
					} catch (Exception e) {}
					segments.put(segmentId, segment);
				}
				segment.setCarrier(getCarrier(course.getCompanyName()));
				segment.setDeparture(addStation(localities, tripPackage.getFrom()));
				segment.setArrival(addStation(localities, tripPackage.getTo()));
				segment.setPrice(getPrice(course.getNormalPrice()));
			}
			container.setTrips(trips);
		}
		if (tripPackage.getError() != null) {
			container.setError(new RestError(tripPackage.getError().getMessage()));
		}
		containers.add(container);
	}

	private Organisation getCarrier(String companyName) {
		if (companyName != null && !companyName.isEmpty()) {
			Organisation carrier = new Organisation(StringUtil.md5(companyName));
			carrier.setName(Lang.EN, companyName);
			return carrier;
		}
		return null;
	}

	public static Price getPrice(BigDecimal price) {
		Price tripPrice = new Price();
		Tariff tariff = new Tariff();
		tariff.setValue(price);
		tripPrice.setCurrency(Currency.PLN);
		tripPrice.setAmount(tariff.getValue());
		tripPrice.setTariff(tariff);
		return tripPrice;
	}

	public static Locality addStation(Map<String, Locality> localities, String id) {
		Locality locality = LocalityServiceController.getLocality(id);
		if (locality == null) {
			return null;
		}
		String localityId = locality.getId();
		try {
			locality = locality.clone();
			locality.setId(null);
		} catch (CloneNotSupportedException e) {
		}
		if (!localities.containsKey(localityId)) {
			localities.put(localityId, locality);
		}
		return new Locality(localityId);
	}

}
