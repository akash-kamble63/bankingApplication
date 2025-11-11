package com.transaction_service.exception;

public class SagaCompensationFailedException extends RuntimeException {
    public SagaCompensationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}