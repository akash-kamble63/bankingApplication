package com.card_service.exception;

public class CardLimitExceededException extends RuntimeException {
    public CardLimitExceededException(String message) {
        super(message);
    }
}