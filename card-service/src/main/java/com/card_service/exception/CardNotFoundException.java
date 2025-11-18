package com.card_service.exception;

public class CardNotFoundException extends RuntimeException {
    public CardNotFoundException(String message) {
        super(message);
    }
}