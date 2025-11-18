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
public class PrepaymentResponse {
	private BigDecimal prepaymentAmount;
	private BigDecimal prepaymentCharges;
	private BigDecimal totalAmount;
	private BigDecimal newOutstandingPrincipal;
	private BigDecimal newEmiAmount;
	private Integer newRemainingTenure;
	private Boolean loanClosed;
}
