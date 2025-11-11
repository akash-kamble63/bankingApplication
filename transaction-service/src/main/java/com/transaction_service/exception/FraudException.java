package com.transaction_service.exception;

public class FraudException extends RuntimeException {
    public FraudException(String message) {
        super(message);
    }
}