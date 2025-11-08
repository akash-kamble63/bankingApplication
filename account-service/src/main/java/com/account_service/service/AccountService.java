package com.account_service.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.account_service.dto.AccountEventResponse;
import com.account_service.dto.AccountFilterRequest;
import com.account_service.dto.AccountResponse;
import com.account_service.dto.AccountStatisticsResponse;
import com.account_service.dto.AccountSummaryResponse;
import com.account_service.dto.BalanceResponse;
import com.account_service.dto.CreateAccountRequest;
import com.account_service.dto.UpdateAccountRequest;
import com.account_service.dto.UserAccountSummary;

public interface AccountService {
	AccountResponse updateAccount(String accountNumber, UpdateAccountRequest request, String updatedBy);
	AccountResponse createAccount(CreateAccountRequest request, String createdBy);

	BalanceResponse creditAccount(String accountNumber, BigDecimal amount, String reason, String transactionRef);

	BalanceResponse debitAccount(String accountNumber, BigDecimal amount, String reason, String transactionRef);

	void closeAccount(String accountNumber, String reason, String closedBy);

	AccountResponse getAccount(String accountNumber);

	BalanceResponse getBalance(String accountNumber);

	List<AccountSummaryResponse> getUserAccounts(Long userId);

	Page<AccountResponse> filterAccounts(AccountFilterRequest filter, Pageable pageable);

	AccountStatisticsResponse getStatistics();

	AccountResponse freezeAccount(String accountNumber, String reason, String updatedBy);

	AccountResponse unfreezeAccount(String accountNumber, String updatedBy);

	AccountResponse activateAccount(String accountNumber, String updatedBy);

	AccountResponse getPrimaryAccount(Long userId);

	UserAccountSummary getUserAccountSummary(Long userId);

	List<AccountEventResponse> getAccountEvents(String accountNumber, Long fromVersion);

	AccountResponse getAccountSnapshot(String accountNumber, String pointInTime);

}
