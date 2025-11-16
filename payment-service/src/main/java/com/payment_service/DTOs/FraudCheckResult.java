package com.payment_service.DTOs;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudCheckResult {
	private boolean blocked;
    private String reason;
    private BigDecimal fraudScore;
    private String riskLevel;
}
