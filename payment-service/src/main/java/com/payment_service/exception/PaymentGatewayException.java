package com.payment_service.exception;

public class PaymentGatewayException extends RuntimeException {
    private String errorCode;
    
    public PaymentGatewayException(String message) {
        super(message);
    }
    
    public PaymentGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public PaymentGatewayException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
