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
public class FraudCheckResponseDTO {
	private String transactionId;
    private FraudStatus status;
    private Double riskScore;
    private List<String> fraudReasons;
    private String recommendation;
    private LocalDateTime checkedAt;
}
