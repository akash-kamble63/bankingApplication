package com.payment_service.DTOs;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.payment_service.enums.PaymentMethod;
import com.payment_service.enums.PaymentStatus;
import com.payment_service.enums.PaymentType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
	private Long id;
	private Long userId;
    private String paymentReference;
    private BigDecimal amount;
    private BigDecimal taxAmount;
    private BigDecimal feeAmount;
    private BigDecimal totalAmount;
    private String currency;
    private PaymentStatus status;
    private PaymentMethod paymentMethod;
    private PaymentType paymentType;
    private String description;
    
    // Masked sensitive data
    private String cardLastFour;
    private String cardBrand;
    private String upiId; // Masked
    private String recipientName;
    
    private String gatewayPaymentId;
    private BigDecimal fraudScore;
    private String riskLevel;
    
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
	
}
