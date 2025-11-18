package com.fraud_detection.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fraud_detection.enums.FraudStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudEventDTO {
	private String transactionId;
    private String accountId;
    private String userId;
    private FraudStatus status;
    private Double riskScore;
    private List<String> fraudReasons;
    private LocalDateTime eventTime;
    private String eventType;
}
