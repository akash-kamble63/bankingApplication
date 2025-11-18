package com.fraud_detection.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEventDTO {
	private String transactionId;
    private String accountId;
    private String userId;
    private BigDecimal amount;
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
    private LocalDateTime timestamp;
}
