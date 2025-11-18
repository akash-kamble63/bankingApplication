package com.loan_service.client;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FraudCheckResult {
	private boolean blocked;
    private BigDecimal fraudScore;
}
