package com.account_service.dto;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.account_service.enums.AccountHolderType;
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
public class AccountResponse {
	 private Long id;
	    private String accountNumber;
	    private Long userId;
	    private String userEmail;
	    private AccountType accountType;
	    private AccountStatus status;
	    private AccountHolderType holderType;
	    private BigDecimal balance;
	    private BigDecimal availableBalance;
	    private BigDecimal minimumBalance;
	    private BigDecimal overdraftLimit;
	    private String currency;
	    private String branchCode;
	    private String ifscCode;
	    private Boolean isPrimary;
	    private BigDecimal interestRate;
	    private LocalDateTime maturityDate;
	    private LocalDateTime createdAt;
	    private LocalDateTime updatedAt;
}
