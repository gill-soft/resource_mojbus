package com.gillsoft.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gillsoft.model.AbstractJsonModel;
import com.gillsoft.model.ServiceItem;

public class OrderIdModel extends AbstractJsonModel {
	
	private static final long serialVersionUID = 5661521517528841959L;
	
	private Map<String, List<ServiceItem>> services = new HashMap<>();

	public Map<String, List<ServiceItem>> getServices() {
		return services;
	}

	public void setServices(Map<String, List<ServiceItem>> services) {
		this.services = services;
	}
	
	@Override
	public OrderIdModel create(String json) {
		return (OrderIdModel) super.create(json);
	}
	
}
