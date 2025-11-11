package com.transaction_service.DTOs;

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
    private BigDecimal totalAmount;
    private BigDecimal totalFees;
    private BigDecimal netAmount;
}
