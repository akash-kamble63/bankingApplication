package com.transaction_service.client;

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
	private String transactionReference;
    private BigDecimal fraudScore;
    private String riskLevel;
    private boolean blocked;
    private String reason;
}
