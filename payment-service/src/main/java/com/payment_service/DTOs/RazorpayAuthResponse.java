package com.payment_service.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RazorpayAuthResponse {
    private String id;
    private String status;
    private String entity;
    private Long amount;
    private String currency;
    private String orderId;
    private String invoiceId;
    private Boolean international;
    private String method;
    private Long amountRefunded;
    private String refundStatus;
    private Boolean captured;
    private String description;
    private String cardId;
    private String bank;
    private String wallet;
    private String vpa;
    private String email;
    private String contact;
    private String tokenId;
    private Long fee;
    private Long tax;
    private String errorCode;
    private String errorDescription;
    private String errorSource;
    private String errorStep;
    private String errorReason;
    private Long createdAt;

    // Custom getter for authorization code
    public String getAuthCode() {
        return id; // Razorpay uses the payment ID as auth code
    }
}
