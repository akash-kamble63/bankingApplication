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
public class LoanSummaryResponse {
	private Long userId;
	private Integer activeLoansCount;
	private BigDecimal totalOutstanding;
	private BigDecimal totalMonthlyEmi;
	private BigDecimal creditUtilization;
}
