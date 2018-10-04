package com.gillsoft.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.ParameterizedTypeReference;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class Response implements Serializable {

	private static final long serialVersionUID = -5510083244244489145L;

	private static final ParameterizedTypeReference<Response> typeRef = new ParameterizedTypeReference<Response>() { };

	private String status;

	private String errorNo;

	private String errorMessage;

	private String test;

	private List<Course> courses;

	private String idReservation;

	private List<String> towns;

	private String ticketUrl;

	private String saved;

	private String normalQty;

	private String reducedQty;

	private String amount;

	private String deductionPercent;

	private String deductionAmount;

	private String idRefund;

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getErrorNo() {
		return errorNo;
	}

	public void setErrorNo(String errorNo) {
		this.errorNo = errorNo;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public String getTest() {
		return test;
	}

	public void setTest(String test) {
		this.test = test;
	}

	public List<Course> getCourses() {
		if (courses == null) {
			courses = new ArrayList<>();
		}
		return courses;
	}

	public void setCourses(List<Course> courses) {
		this.courses = courses;
	}

	public String getIdReservation() {
		return idReservation;
	}

	public void setIdReservation(String idReservation) {
		this.idReservation = idReservation;
	}

	public List<String> getTowns() {
		if (towns == null) {
			towns = new ArrayList<>();
		}
		return towns;
	}

	public void setTowns(List<String> towns) {
		this.towns = towns;
	}

	public String getTicketUrl() {
		return ticketUrl;
	}

	public void setTicketUrl(String ticketUrl) {
		this.ticketUrl = ticketUrl;
	}

	public String getSaved() {
		return saved;
	}

	public void setSaved(String saved) {
		this.saved = saved;
	}

	public String getNormalQty() {
		return normalQty;
	}

	public void setNormalQty(String normalQty) {
		this.normalQty = normalQty;
	}

	public String getReducedQty() {
		return reducedQty;
	}

	public void setReducedQty(String reducedQty) {
		this.reducedQty = reducedQty;
	}

	public String getAmount() {
		return amount;
	}

	public void setAmount(String amount) {
		this.amount = amount;
	}

	public String getDeductionPercent() {
		return deductionPercent;
	}

	public void setDeductionPercent(String deductionPercent) {
		this.deductionPercent = deductionPercent;
	}

	public String getDeductionAmount() {
		return deductionAmount;
	}

	public void setDeductionAmount(String deductionAmount) {
		this.deductionAmount = deductionAmount;
	}

	public String getIdRefund() {
		return idRefund;
	}

	public void setIdRefund(String idRefund) {
		this.idRefund = idRefund;
	}

	public static ParameterizedTypeReference<Response> getTypeReference() {
		return Response.typeRef;
	}

}
