package com.transaction_service.DTO;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionSummaryResponse {
	private Long totalTransactions;
	private BigDecimal totalDeposits;
	private BigDecimal totalWithdrawals;
	private BigDecimal totalTransfers;
	private BigDecimal netAmount;
	private Long successfulTransactions;
	private Long failedTransactions;
}
