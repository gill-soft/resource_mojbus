package com.gillsoft.client;

import java.io.Serializable;
import java.util.List;

import org.springframework.core.ParameterizedTypeReference;

import com.gillsoft.model.request.TripSearchRequest;

public class TripPackage implements Serializable {

	private static final long serialVersionUID = -4074866420438921827L;

	private static final ParameterizedTypeReference<TripPackage> typeRef = new ParameterizedTypeReference<TripPackage>() { };

	private TripSearchRequest request;

	private boolean inProgress = true;

	private ResponseError error;

	private boolean continueSearch = false;

	private List<com.gillsoft.model.Course> courseList;

	private String from;

	private String to;

	public TripSearchRequest getRequest() {
		return request;
	}

	public void setRequest(TripSearchRequest request) {
		this.request = request;
	}

	public boolean isInProgress() {
		return inProgress;
	}

	public void setInProgress(boolean inProgress) {
		this.inProgress = inProgress;
	}

	public ResponseError getError() {
		return error;
	}

	public void setError(ResponseError error) {
		this.error = error;
	}

	public boolean isContinueSearch() {
		return continueSearch;
	}

	public void setContinueSearch(boolean continueSearch) {
		this.continueSearch = continueSearch;
	}
	
	public List<com.gillsoft.model.Course> getCourseList() {
		return courseList;
	}

	public void setCourseList(List<com.gillsoft.model.Course> courseList) {
		this.courseList = courseList;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public static ParameterizedTypeReference<TripPackage> getTypeReference() {
		return typeRef;
	}

}
