package com.loan_service.client;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FraudServiceClient {
	public FraudCheckResult checkLoanApplication(Long userId, BigDecimal amount, BigDecimal income) {
		return FraudCheckResult.builder().blocked(false).fraudScore(new BigDecimal("10.0")).build();
	}
}
