package com.account_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BalanceResponse {
	private String accountNumber;
	private BigDecimal balance;
	private BigDecimal availableBalance;
	private BigDecimal minimumBalance;
	private BigDecimal overdraftLimit;
	private BigDecimal totalHolds;
	private String currency;
	private LocalDateTime asOfDate;
}
