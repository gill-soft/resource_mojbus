package com.gillsoft.client;

import java.util.Date;

import com.gillsoft.model.AbstractJsonModel;

public class TripIdModel extends AbstractJsonModel {

	private static final long serialVersionUID = -8580474896771951668L;

	private String idCourse;

	private String startStopNr;

	private String finishStopNr;

	private Date dateDispatch;

	private Date dateArrival;

	public TripIdModel() {

	}

	public TripIdModel(String idCourse, String startStopNr, String finishStopNr, Date dateDispatch, Date dateArrival) {
		this.idCourse = idCourse;
		this.startStopNr = startStopNr;
		this.finishStopNr = finishStopNr;
		this.dateDispatch = dateDispatch;
		this.dateArrival = dateArrival;
	}

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

	public Date getDateDispatch() {
		return dateDispatch;
	}

	public void setDateDispatch(Date dateDispatch) {
		this.dateDispatch = dateDispatch;
	}

	public Date getDateArrival() {
		return dateArrival;
	}

	public void setDateArrival(Date dateArrival) {
		this.dateArrival = dateArrival;
	}

}
