package com.payment_service.DTOs;

import java.math.BigDecimal;


import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardPaymentRequest {

	@NotNull(message = "User ID is required")
    private Long userId;
    
    private Long accountId; // Optional - can pay without linked account
    
    private Long merchantId;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.0", message = "Amount must be at least 1")
    @DecimalMax(value = "1000000.0", message = "Amount exceeds limit")
    private BigDecimal amount;
    
    @Pattern(regexp = "^[A-Z]{3}$", message = "Invalid currency")
    private String currency = "INR";
    
    @NotBlank(message = "Card token is required")
    private String cardToken; // Tokenized card (PCI-DSS compliant)
    
    @Pattern(regexp = "^[0-9]{4}$", message = "Invalid card last four")
    private String cardLastFour;
    
    @Pattern(regexp = "^(VISA|MASTERCARD|AMEX|RUPAY)$")
    private String cardBrand;
    
    @NotBlank(message = "Gateway name is required")
    private String gatewayName; // RAZORPAY, STRIPE, etc.
    
    @Size(max = 500)
    private String description;
    
    private String deviceFingerprint;
    
    private String idempotencyKey;
}
