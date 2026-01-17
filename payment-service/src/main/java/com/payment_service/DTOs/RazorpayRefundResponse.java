package com.payment_service.DTOs;

import lombok.Data;

@Data
public class RazorpayRefundResponse {
    private String id;
    private String entity;
    private Long amount;
    private String currency;
    private String paymentId;
    private String status;
    private String errorCode;
    private String errorDescription;
    private Long createdAt;
}