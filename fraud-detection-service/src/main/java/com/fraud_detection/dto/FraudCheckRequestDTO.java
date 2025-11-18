package com.fraud_detection.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudCheckRequestDTO {
	@NotBlank(message = "Transaction ID is required")
    private String transactionId;
    
    @NotBlank(message = "Account ID is required")
    private String accountId;
    
    @NotBlank(message = "User ID is required")
    private String userId;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;
    
    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters")
    private String currency;
    
    private String transactionType;
    private String merchantName;
    private String merchantCategory;
    private String locationCountry;
    private String locationCity;
    private Double latitude;
    private Double longitude;
    private String deviceId;
    private String ipAddress;
}
