package com.gillsoft.client;

public class ResponseError extends Exception {

	private static final long serialVersionUID = -486549465922260485L;

	public ResponseError(String message) {
		super(message);
	}
	
	public ResponseError(String message, Exception nestedException) {
		super(message, nestedException);
	}
	
}
