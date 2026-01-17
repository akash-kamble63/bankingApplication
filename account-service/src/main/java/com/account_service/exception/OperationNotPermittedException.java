package com.account_service.exception;

public class OperationNotPermittedException extends RuntimeException {

    public OperationNotPermittedException(String message) {
        super(message);
    }

    public OperationNotPermittedException(String message, Throwable cause) {
        super(message, cause);
    }
}