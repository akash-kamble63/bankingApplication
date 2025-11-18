package com.fraud_detection.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudStatisticsDTO {
	private Long totalChecks;
    private Long approvedCount;
    private Long flaggedCount;
    private Long blockedCount;
    private Long manualReviewCount;
    private Double averageRiskScore;
    private BigDecimal totalFlaggedAmount;
    private BigDecimal totalBlockedAmount;
}
