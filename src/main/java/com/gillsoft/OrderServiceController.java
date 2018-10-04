package com.gillsoft;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import com.gillsoft.abstract_rest_service.AbstractOrderService;
import com.gillsoft.client.OrderIdModel;
import com.gillsoft.client.RestClient;
import com.gillsoft.client.TripIdModel;
import com.gillsoft.model.Currency;
import com.gillsoft.model.Customer;
import com.gillsoft.model.Document;
import com.gillsoft.model.DocumentType;
import com.gillsoft.model.Price;
import com.gillsoft.model.RestError;
import com.gillsoft.model.ServiceItem;
import com.gillsoft.model.Tariff;
import com.gillsoft.model.request.OrderRequest;
import com.gillsoft.model.response.OrderResponse;
import com.gillsoft.model.Response;

@RestController
public class OrderServiceController extends AbstractOrderService {
	
	@Autowired
	private RestClient client;

	@Override
	public OrderResponse addServicesResponse(OrderRequest request) {
		throw RestClient.createUnavailableMethod();
	}

	@Override
	public OrderResponse bookingResponse(String orderId) {
		throw RestClient.createUnavailableMethod();
	}

	@Override
	public OrderResponse cancelResponse(String orderId) {
		List<ServiceItem> resultItems = new ArrayList<>();
		OrderResponse response = new OrderResponse();
		// преобразовываем ид заказа в объект
		OrderIdModel orderIdModel = new OrderIdModel().create(orderId);
		response.setServices(new ArrayList<>());
		orderIdModel.getServices().forEach((key, value) -> {
			try {
				client.cancel(key);
				addServiceItem(resultItems, value, true, null);
			} catch (Exception e) {
				addServiceItem(resultItems, value, false, new RestError(e.getMessage()));
			}
		});
		response.setOrderId(orderId);
		response.setServices(resultItems);
		return response;
	}

	@Override
	public OrderResponse createResponse(OrderRequest request) {
		// формируем ответ
		OrderResponse response = new OrderResponse();
		// копия для определения пассажиров
		List<ServiceItem> items = new ArrayList<>();
		response.setServices(request.getServices());
		items.addAll(request.getServices());
		// список билетов
		OrderIdModel orderId = new OrderIdModel();
		// группируем пассажиров по id сегмента
		Map<String, List<ServiceItem>> serviceMap = new HashMap<>();
		request.getServices().stream().forEach(c -> {
			String cutomerId = c.getCustomer().getId();
			c.setCustomer(request.getCustomers().get(c.getCustomer().getId()));
			if (c.getCustomer() != null) {
				c.getCustomer().setId(cutomerId);
			} else {
				c.setCustomer(new Customer(cutomerId));
			}
		});
		request.getServices().stream().map(m -> m.getSegment().getId()).collect(Collectors.toSet())
				.forEach(a -> serviceMap.put(a, request.getServices().stream()
						.filter(f -> f.getSegment().getId().equals(a)).map(m -> m).collect(Collectors.toList())));
		// по каждому сегменту формируем отдельную бронь
		serviceMap.entrySet().stream().forEach(entry -> {
			try {
				Response reserveResponse = client.reserve(entry);
				if (orderId.getServices() == null) {
					orderId.setServices(new HashMap<>());
				}
				orderId.getServices().put(reserveResponse.getIdReservation(), entry.getValue());
				request.getServices().stream().filter(f -> f.getSegment().getId().equals(entry.getKey()))
						.forEach(c -> c.setConfirmed(true));
			} catch (Exception e) {
				RestError restError = new RestError(e.getMessage());
				request.getServices().stream().filter(f -> f.getSegment().getId().equals(entry.getKey())).forEach(c -> {
					c.setError(restError);
					c.setConfirmed(false);
				});
			}
		});
		response.setOrderId(orderId.asString());
		return response;
	}

	@Override
	public OrderResponse getPdfDocumentsResponse(OrderRequest request) {
		// формируем ответ
		OrderResponse response = new OrderResponse();
		response.setCustomers(request.getCustomers());
		response.setServices(request.getServices());
		// преобразовываем ид заказа в объект
		OrderIdModel orderIdModel = new OrderIdModel().create(request.getOrderId());
		// получаем билеты от ресурса и формируем ответ
		orderIdModel.getServices().forEach((key, value) -> {
			if (response.getDocuments() == null) {
				response.setDocuments(new ArrayList<>());
			}
			try {
				Document ticket = new Document();
				ticket.setType(DocumentType.TICKET);
				ticket.setBase64(client.getTicketPdf(client.getTicketUrl(key)));
				response.getDocuments().add(ticket);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		return response;
	}

	@Override
	public OrderResponse getResponse(String orderId) {
		throw RestClient.createUnavailableMethod();
	}

	@Override
	public OrderResponse getServiceResponse(String serviceId) {
		throw RestClient.createUnavailableMethod();
	}

	@Override
	public OrderResponse confirmResponse(String orderId) {
		// формируем ответ
		OrderResponse response = new OrderResponse();
		List<ServiceItem> resultItems = new ArrayList<>();
		// преобразовываем ид заказа в объект
		OrderIdModel orderIdModel = new OrderIdModel().create(orderId);
		// выкупаем заказы и формируем ответ
		orderIdModel.getServices().forEach((key, value) -> {
			try {
				Response confirmResponse = client.confirm(key);
				addServiceItem(resultItems, value, true, null);
				if (confirmResponse.getTicketUrl() != null) {
					saveTicketUrl(confirmResponse, value);
				}
			} catch (Exception e) {
				addServiceItem(resultItems, value, false, new RestError(e.getMessage()));
			}
		});
		response.setOrderId(orderId);
		response.setServices(resultItems);
		return response;
	}
	
	private void saveTicketUrl(Response confirmResponse, List<ServiceItem> value) {
		try {
			TripIdModel trip = (TripIdModel) new TripIdModel().create(value.get(0).getSegment().getId());
			Calendar c = Calendar.getInstance();
			c.setTime(trip.getDateArrival());
			c.add(Calendar.DATE, 7);
			client.saveTicketUrl(confirmResponse.getIdReservation(), confirmResponse.getTicketUrl(), c.getTimeInMillis() - System.currentTimeMillis());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void addServiceItem(List<ServiceItem> resultItems, List<ServiceItem> value, boolean confirmed,
			RestError error) {
		value.stream().forEach(service -> {
			service.setConfirmed(confirmed);
			service.setError(error);
			resultItems.add(service);
		});
	}

	@Override
	public OrderResponse removeServicesResponse(OrderRequest request) {
		throw RestClient.createUnavailableMethod();
	}

	@Override
	public OrderResponse returnServicesResponse(OrderRequest request) {
		// формируем ответ
		OrderResponse response = new OrderResponse();
		response.setOrderId(request.getOrderId());
		response.setCustomers(request.getCustomers());
		response.setServices(request.getServices());
		// преобразовываем ид заказа в объект
		OrderIdModel orderIdModel = new OrderIdModel().create(request.getOrderId());
		// получаем билеты от ресурса и формируем ответ
		orderIdModel.getServices().forEach((key, value) -> {
			try {
				Response refundResponse = client.refund(key, value.size());
				response.getServices().stream().filter(f -> f.getSegment().getId().equals(value.get(0).getSegment().getId())).forEach(c -> {
					c.setConfirmed(true);
					c.setPrice(getReturnPrice(refundResponse.getAmount(), value.size()));
				});
			} catch (Exception e) {
				response.getServices().stream().filter(f -> f.getSegment().getId().equals(value.get(0).getSegment().getId())).forEach(c -> {
					c.setConfirmed(false);
					c.setError(new RestError(e.getMessage()));
				});
			}
		});
		return response;
	}

	@Override
	public OrderResponse updateCustomersResponse(OrderRequest request) {
		throw RestClient.createUnavailableMethod();
	}

	@Override
	public OrderResponse prepareReturnServicesResponse(OrderRequest request) {
		// формируем ответ
		OrderResponse response = new OrderResponse();
		response.setOrderId(request.getOrderId());
		response.setCustomers(request.getCustomers());
		response.setServices(request.getServices());
		// преобразовываем ид заказа в объект
		OrderIdModel orderIdModel = new OrderIdModel().create(request.getOrderId());
		// получаем билеты от ресурса и формируем ответ
		orderIdModel.getServices().forEach((key, value) -> {
			try {
				Response refundResponse = client.refundPrepare(key, value.size());
				response.getServices().stream().filter(f -> f.getSegment().getId().equals(value.get(0).getSegment().getId())).forEach(c -> {
					c.setConfirmed(true);
					c.setPrice(getReturnPrice(refundResponse.getAmount(), value.size()));
				});
			} catch (Exception e) {
				response.getServices().stream().filter(f -> f.getSegment().getId().equals(value.get(0).getSegment().getId())).forEach(c -> {
					c.setConfirmed(false);
					c.setError(new RestError(e.getMessage()));
				});
			}
		});
		return response;
	}

	private Price getReturnPrice(String totalAmount, int serviceQty) {
		Price returnPrice = new Price();
		returnPrice.setAmount(new BigDecimal(totalAmount).divide(new BigDecimal(serviceQty)));
		returnPrice.setCurrency(Currency.PLN);
		returnPrice.setTariff(new Tariff());
		returnPrice.getTariff().setValue(returnPrice.getAmount());
		return returnPrice;
	}

}
