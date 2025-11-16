package com.payment_service.DTOs;

import java.math.BigDecimal;

import com.payment_service.enums.PaymentMethod;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSagaData {
	private String sagaId;
    private String paymentReference;
    private Long userId;
    private Long accountId;
    private Long merchantId;
    private BigDecimal amount;
    private String currency;
    private PaymentMethod paymentMethod;
    
    // Card payment fields
    private String cardToken;
    private String gatewayName;
    
    // UPI fields
    private String upiId;
    private String upiTransactionId;
    
    // Bill payment fields
    private String billerId;
    private String billNumber;
    
    // Step tracking
    private boolean fraudChecked;
    private boolean fundsReserved;
    private boolean paymentAuthorized;
    private boolean paymentCaptured;
    private boolean accountDebited;
    private boolean merchantCredited;
    
    // Compensation data
    private String holdReference;
    private String gatewayPaymentId;
    private String authorizationCode;
    private String externalTransactionId;
    
    // Fraud data
    private BigDecimal fraudScore;
    
    // Error data
    private String gatewayErrorCode;
    private String gatewayErrorMessage;
}
