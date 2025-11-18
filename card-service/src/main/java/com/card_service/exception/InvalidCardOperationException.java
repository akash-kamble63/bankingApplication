package com.card_service.exception;

public class InvalidCardOperationException extends RuntimeException {
	public InvalidCardOperationException(String message) {
		super(message);
	}
}