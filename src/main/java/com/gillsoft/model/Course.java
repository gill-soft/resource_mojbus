package com.gillsoft.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.gillsoft.util.StringUtil;

public class Course implements Serializable {

	private static final long serialVersionUID = -4076281118869350516L;

	private String idCourse;

	private String startStopNr;

	private String finishStopNr;

	private int seatsAvailable;

	@JsonFormat(pattern=StringUtil.FULL_DATE_FORMAT)
	private Date startDateTime;

	@JsonFormat(pattern=StringUtil.FULL_DATE_FORMAT)
	private Date finishDateTime;

	private String startTown;

	private String startAddress;

	private String finishTown;

	private String finishAddress;

	private BigDecimal normalPrice;

	private BigDecimal reducedPrice;

	private String totalPrice;

	private String reducedEntitledHtml;

	private String company;
	
	private String companyName;

	public String getIdCourse() {
		return idCourse;
	}

	public void setIdCourse(String idCourse) {
		this.idCourse = idCourse;
	}

	public String getStartStopNr() {
		return startStopNr;
	}

	public void setStartStopNr(String startStopNr) {
		this.startStopNr = startStopNr;
	}

	public String getFinishStopNr() {
		return finishStopNr;
	}

	public void setFinishStopNr(String finishStopNr) {
		this.finishStopNr = finishStopNr;
	}

	public int getSeatsAvailable() {
		return seatsAvailable;
	}

	public void setSeatsAvailable(int seatsAvailable) {
		this.seatsAvailable = seatsAvailable;
	}

	public Date getStartDateTime() {
		return startDateTime;
	}

	public void setStartDateTime(Date startDateTime) {
		this.startDateTime = startDateTime;
	}

	public Date getFinishDateTime() {
		return finishDateTime;
	}

	public void setFinishDateTime(Date finishDateTime) {
		this.finishDateTime = finishDateTime;
	}

	public String getStartTown() {
		return startTown;
	}

	public void setStartTown(String startTown) {
		this.startTown = startTown;
	}

	public String getStartAddress() {
		return startAddress;
	}

	public void setStartAddress(String startAddress) {
		this.startAddress = startAddress;
	}

	public String getFinishTown() {
		return finishTown;
	}

	public void setFinishTown(String finishTown) {
		this.finishTown = finishTown;
	}

	public String getFinishAddress() {
		return finishAddress;
	}

	public void setFinishAddress(String finishAddress) {
		this.finishAddress = finishAddress;
	}

	public BigDecimal getNormalPrice() {
		return normalPrice;
	}

	public void setNormalPrice(BigDecimal normalPrice) {
		this.normalPrice = normalPrice;
	}

	public BigDecimal getReducedPrice() {
		return reducedPrice;
	}

	public void setReducedPrice(BigDecimal reducedPrice) {
		this.reducedPrice = reducedPrice;
	}

	public String getTotalPrice() {
		return totalPrice;
	}

	public void setTotalPrice(String totalPrice) {
		this.totalPrice = totalPrice;
	}

	public String getReducedEntitledHtml() {
		return reducedEntitledHtml;
	}

	public void setReducedEntitledHtml(String reducedEntitledHtml) {
		this.reducedEntitledHtml = reducedEntitledHtml;
	}

	public String getCompany() {
		return company;
	}

	public void setCompany(String company) {
		this.company = company;
	}

	public String getCompanyName() {
		return companyName;
	}

	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}

}
