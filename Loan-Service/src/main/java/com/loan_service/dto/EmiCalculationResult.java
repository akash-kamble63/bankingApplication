package com.loan_service.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmiCalculationResult {
	private BigDecimal emiAmount;
	private BigDecimal lastEmiAmount;
	private BigDecimal totalPayment;
	private BigDecimal totalInterest;
	private BigDecimal principalAmount;
}
