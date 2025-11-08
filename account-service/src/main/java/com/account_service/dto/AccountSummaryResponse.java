package com.account_service.dto;

import java.math.BigDecimal;

import com.account_service.enums.AccountStatus;
import com.account_service.enums.AccountType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountSummaryResponse {
	private Long id;
	private String accountNumber;
	private AccountType accountType;
	private AccountStatus status;
	private BigDecimal balance;
	private BigDecimal availableBalance;
	private String currency;
	private Boolean isPrimary;
}
