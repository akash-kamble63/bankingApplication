package com.payment_service.exception;

public class IdempotencyConflictException extends IdempotencyException {
    public IdempotencyConflictException(String message) {
        super(message);
    }
}