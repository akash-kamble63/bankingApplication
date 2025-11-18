package com.account_service.service.implementation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.account_service.dto.AccountEventResponse;
import com.account_service.dto.AccountFilterRequest;
import com.account_service.dto.AccountResponse;
import com.account_service.dto.AccountStatisticsResponse;
import com.account_service.dto.AccountStatusChangedEvent;
import com.account_service.dto.AccountSummaryResponse;
import com.account_service.dto.BalanceResponse;
import com.account_service.dto.BalanceUpdatedEvent;
import com.account_service.dto.CreateAccountRequest;
import com.account_service.dto.UpdateAccountRequest;
import com.account_service.dto.UserAccountSummary;
import com.account_service.enums.AccountStatus;
import com.account_service.enums.AccountType;
import com.account_service.enums.AuditAction;
import com.account_service.exception.ResourceConflictException;
import com.account_service.exception.ResourceNotFoundException;
import com.account_service.model.Account;
import com.account_service.model.AccountEventStore;
import com.account_service.patterns.AccountCreatedEvent;
import com.account_service.repository.AccountRepository;
import com.account_service.service.AccountService;
import com.account_service.service.AuditService;
import com.account_service.service.EventSourcingService;
import com.account_service.service.OutboxService;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {
	private final AccountRepository accountRepository;
	private final OutboxService outboxService;
	private final EventSourcingService eventSourcingService;
	private final AuditService auditService;

	/**
	 * Create new account (COMMAND) Uses Outbox pattern, Event Sourcing, and
	 * publishes events
	 */
	@Override
	@Transactional
	@Retry(name = "database")
	@CacheEvict(value = { "accountList", "statistics" }, allEntries = true)
	public AccountResponse createAccount(CreateAccountRequest request, String createdBy) {
		log.info("Creating account for user: {}", request.getUserId());

		// Validate user doesn't have too many accounts
		long existingAccounts = accountRepository.countByUserId(request.getUserId());
		if (existingAccounts >= 5) {
			throw new ResourceConflictException("User already has maximum number of accounts");
		}

		// Validate initial deposit meets minimum balance
		BigDecimal minimumBalance = getMinimumBalanceForType(request.getAccountType());
		if (request.getInitialDeposit().compareTo(minimumBalance) < 0) {
			throw new IllegalArgumentException(
					"Initial deposit must be at least " + minimumBalance + " " + request.getCurrency());
		}

		try {
			// Generate account number
			String accountNumber = generateAccountNumber();

			// Generate IFSC code
			String ifscCode = generateIfscCode(request.getBranchCode());

			// Create account entity
			Account account = Account.builder().accountNumber(accountNumber).userId(request.getUserId())
					.userEmail(request.getUserEmail()).accountType(request.getAccountType())
					.status(AccountStatus.ACTIVE).holderType(request.getHolderType())
					.balance(request.getInitialDeposit()).availableBalance(request.getInitialDeposit())
					.minimumBalance(minimumBalance).overdraftLimit(getOverdraftLimit(request.getAccountType()))
					.currency(request.getCurrency()).branchCode(request.getBranchCode()).ifscCode(ifscCode)
					.isPrimary(request.getIsPrimary()).interestRate(getInterestRate(request.getAccountType()))
					.createdBy(createdBy).build();

			// Save account
			account = accountRepository.save(account);
			log.info("Account created: {}", accountNumber);

			// Event Sourcing: Store event
			AccountCreatedEvent event = buildAccountCreatedEvent(account);
			eventSourcingService.storeEvent(accountNumber, "AccountCreated", event, request.getUserId(),
					UUID.randomUUID().toString(), null);

			// Outbox Pattern: Save event for Kafka publishing
			outboxService.saveEvent("ACCOUNT", accountNumber, "AccountCreated", "banking.account.created", event);

			// Audit log
			auditService.logSuccess(AuditAction.USER_CREATED, request.getUserId(), "ACCOUNT", accountNumber, request);

			return mapToResponse(account);

		} catch (Exception e) {
			log.error("Failed to create account: {}", e.getMessage(), e);
			auditService.logFailure(AuditAction.USER_CREATED, request.getUserId(), "ACCOUNT", null, request,
					e.getMessage());
			throw new RuntimeException("Failed to create account", e);
		}
	}

	/**
	 * Update account (COMMAND)
	 */
	@Override
	@Transactional
	@CacheEvict(value = { "accountDetails", "accountList" }, allEntries = true)
	public AccountResponse updateAccount(String accountNumber, UpdateAccountRequest request, String updatedBy) {
		log.info("Updating account: {}", accountNumber);

		Account account = getAccountEntity(accountNumber);
		AccountStatus previousStatus = account.getStatus();

		// Update fields
		if (request.getStatus() != null) {
			account.setStatus(request.getStatus());
		}
		if (request.getIsPrimary() != null) {
			account.setIsPrimary(request.getIsPrimary());
		}
		if (request.getBranchCode() != null) {
			account.setBranchCode(request.getBranchCode());
		}
		account.setUpdatedBy(updatedBy);

		account = accountRepository.save(account);

		// If status changed, store event
		if (!previousStatus.equals(account.getStatus())) {
			AccountStatusChangedEvent event = AccountStatusChangedEvent.builder().accountNumber(accountNumber)
					.previousStatus(previousStatus).newStatus(account.getStatus()).reason("Manual update").build();

			eventSourcingService.storeEvent(accountNumber, "AccountStatusChanged", event, account.getUserId(),
					UUID.randomUUID().toString(), null);

			outboxService.saveEvent("ACCOUNT", accountNumber, "AccountStatusChanged", "banking.account.updated", event);
		}

		auditService.logSuccess(AuditAction.USER_UPDATED, account.getUserId(), "ACCOUNT", accountNumber, request);

		return mapToResponse(account);
	}

	/**
	 * Credit account (COMMAND)
	 */
	@Override
	@Transactional
	@Retry(name = "database")
	@CacheEvict(value = { "accountDetails", "balance" }, key = "#accountNumber")
	public BalanceResponse creditAccount(String accountNumber, BigDecimal amount, String reason,
			String transactionRef) {
		log.info("Crediting account: {} with amount: {}", accountNumber, amount);

		Account account = accountRepository.findByAccountNumberForUpdate(accountNumber)
				.orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountNumber));

		if (account.getStatus() != AccountStatus.ACTIVE) {
			throw new IllegalStateException("Account is not active");
		}

		BigDecimal previousBalance = account.getBalance();
		account.setBalance(account.getBalance().add(amount));
		account.setAvailableBalance(account.getAvailableBalance().add(amount));
		accountRepository.save(account);

		// Store balance update event
		BalanceUpdatedEvent event = BalanceUpdatedEvent.builder().accountNumber(accountNumber)
				.previousBalance(previousBalance).newBalance(account.getBalance()).amount(amount).operation("CREDIT")
				.reason(reason).transactionReference(transactionRef).build();

		eventSourcingService.storeEvent(accountNumber, "BalanceUpdated", event, account.getUserId(),
				UUID.randomUUID().toString(), transactionRef);

		outboxService.saveEvent("ACCOUNT", accountNumber, "BalanceUpdated", "banking.balance.updated", event);

		return buildBalanceResponse(account);
	}

	/**
	 * Debit account (COMMAND)
	 */
	@Override
	@Transactional
	@Retry(name = "database")
	@CacheEvict(value = { "accountDetails", "balance" }, key = "#accountNumber")
	public BalanceResponse debitAccount(String accountNumber, BigDecimal amount, String reason, String transactionRef) {
		log.info("Debiting account: {} with amount: {}", accountNumber, amount);

		Account account = accountRepository.findByAccountNumberForUpdate(accountNumber)
				.orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountNumber));

		if (account.getStatus() != AccountStatus.ACTIVE) {
			throw new IllegalStateException("Account is not active");
		}

		// Check sufficient balance
		BigDecimal availableWithOverdraft = account.getAvailableBalance().add(account.getOverdraftLimit());
		if (availableWithOverdraft.compareTo(amount) < 0) {
			throw new IllegalStateException("Insufficient balance");
		}

		BigDecimal previousBalance = account.getBalance();
		account.setBalance(account.getBalance().subtract(amount));
		account.setAvailableBalance(account.getAvailableBalance().subtract(amount));
		accountRepository.save(account);

		// Store balance update event
		BalanceUpdatedEvent event = BalanceUpdatedEvent.builder().accountNumber(accountNumber)
				.previousBalance(previousBalance).newBalance(account.getBalance()).amount(amount).operation("DEBIT")
				.reason(reason).transactionReference(transactionRef).build();

		eventSourcingService.storeEvent(accountNumber, "BalanceUpdated", event, account.getUserId(),
				UUID.randomUUID().toString(), transactionRef);

		outboxService.saveEvent("ACCOUNT", accountNumber, "BalanceUpdated", "banking.balance.updated", event);

		return buildBalanceResponse(account);
	}

	/**
	 * Close account (COMMAND)
	 */
	@Override
	@Transactional
	@CacheEvict(value = { "accountDetails", "accountList" }, allEntries = true)
	public void closeAccount(String accountNumber, String reason, String closedBy) {
		log.info("Closing account: {}", accountNumber);

		Account account = getAccountEntity(accountNumber);

		if (account.getStatus() == AccountStatus.CLOSED) {
			throw new IllegalStateException("Account already closed");
		}

		if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
			throw new IllegalStateException("Cannot close account with non-zero balance");
		}

		account.setStatus(AccountStatus.CLOSED);
		account.setClosureReason(reason);
		account.setClosedAt(LocalDateTime.now());
		account.setUpdatedBy(closedBy);
		accountRepository.save(account);

		// Store event
		outboxService.saveEvent("ACCOUNT", accountNumber, "AccountClosed", "banking.account.closed",
				Map.of("accountNumber", accountNumber, "reason", reason, "closedAt", LocalDateTime.now()));

		auditService.logSuccess(AuditAction.USER_DELETED, account.getUserId(), "ACCOUNT", accountNumber,
				Map.of("reason", reason));
	}

	// ============= QUERY METHODS (READ OPERATIONS - CQRS) =============

	/**
	 * Get account by number (QUERY)
	 */
	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = "accountDetails", key = "#accountNumber")
	@CircuitBreaker(name = "userService", fallbackMethod = "getAccountFallback")
	public AccountResponse getAccount(String accountNumber) {
		log.debug("Fetching account: {}", accountNumber);
		Account account = getAccountEntity(accountNumber);
		return mapToResponse(account);
	}

	/**
	 * Get balance (QUERY)
	 */
	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = "balance", key = "#accountNumber")
	public BalanceResponse getBalance(String accountNumber) {
		Account account = getAccountEntity(accountNumber);
		return buildBalanceResponse(account);
	}

	/**
	 * Get user accounts (QUERY)
	 */
	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = "accountList", key = "#userId")
	public List<AccountSummaryResponse> getUserAccounts(Long userId) {
		return accountRepository.findByUserId(userId).stream().map(this::mapToSummary).collect(Collectors.toList());
	}

	/**
	 * Filter accounts with advanced criteria (QUERY)
	 */
	@Override
	@Transactional(readOnly = true)
	public Page<AccountResponse> filterAccounts(AccountFilterRequest filter, Pageable pageable) {
		Specification<Account> spec = buildSpecification(filter);
		return accountRepository.findAll(spec, pageable).map(this::mapToResponse);
	}

	/**
	 * Get statistics (QUERY)
	 */
	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = "statistics")
	public AccountStatisticsResponse getStatistics() {
		// Implementation for statistics
		return AccountStatisticsResponse.builder().totalAccounts(accountRepository.count())
				.activeAccounts(accountRepository.countByStatus(AccountStatus.ACTIVE)).build();
	}

	// ============= HELPER METHODS =============

	private Account getAccountEntity(String accountNumber) {
		return accountRepository.findByAccountNumber(accountNumber)
				.orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountNumber));
	}

	private String generateAccountNumber() {
		Random random = new Random();
		String accountNumber;
		do {
			accountNumber = "ACC" + String.format("%013d", random.nextLong() & Long.MAX_VALUE).substring(0, 13);
		} while (accountRepository.existsByAccountNumber(accountNumber));
		return accountNumber;
	}

	private String generateIfscCode(String branchCode) {
		return "BANK0" + (branchCode != null ? branchCode : "000000");
	}

	private BigDecimal getMinimumBalanceForType(AccountType type) {
		return switch (type) {
		case SAVINGS -> new BigDecimal("1000.00");
		case CURRENT -> new BigDecimal("5000.00");
		case SALARY -> BigDecimal.ZERO;
		case STUDENT -> new BigDecimal("500.00");
		default -> new BigDecimal("1000.00");
		};
	}

	private BigDecimal getOverdraftLimit(AccountType type) {
		return switch (type) {
		case CURRENT -> new BigDecimal("50000.00");
		case SALARY -> new BigDecimal("10000.00");
		default -> BigDecimal.ZERO;
		};
	}

	private BigDecimal getInterestRate(AccountType type) {
		return switch (type) {
		case SAVINGS -> new BigDecimal("4.00");
		case FIXED_DEPOSIT -> new BigDecimal("6.50");
		case RECURRING_DEPOSIT -> new BigDecimal("5.50");
		default -> BigDecimal.ZERO;
		};
	}

	private AccountCreatedEvent buildAccountCreatedEvent(Account account) {
		return AccountCreatedEvent.builder().accountNumber(account.getAccountNumber()).userId(account.getUserId())
				.userEmail(account.getUserEmail()).accountType(account.getAccountType()).status(account.getStatus())
				.initialBalance(account.getBalance()).currency(account.getCurrency())
				.branchCode(account.getBranchCode()).build();
	}

	private BalanceResponse buildBalanceResponse(Account account) {
		return BalanceResponse.builder().accountNumber(account.getAccountNumber()).balance(account.getBalance())
				.availableBalance(account.getAvailableBalance()).minimumBalance(account.getMinimumBalance())
				.overdraftLimit(account.getOverdraftLimit()).currency(account.getCurrency())
				.asOfDate(LocalDateTime.now()).build();
	}

	private AccountResponse mapToResponse(Account account) {
		return AccountResponse.builder().id(account.getId()).accountNumber(account.getAccountNumber())
				.userId(account.getUserId()).userEmail(account.getUserEmail()).accountType(account.getAccountType())
				.status(account.getStatus()).holderType(account.getHolderType()).balance(account.getBalance())
				.availableBalance(account.getAvailableBalance()).minimumBalance(account.getMinimumBalance())
				.overdraftLimit(account.getOverdraftLimit()).currency(account.getCurrency())
				.branchCode(account.getBranchCode()).ifscCode(account.getIfscCode()).isPrimary(account.getIsPrimary())
				.interestRate(account.getInterestRate()).maturityDate(account.getMaturityDate())
				.createdAt(account.getCreatedAt()).updatedAt(account.getUpdatedAt()).build();
	}

	private AccountSummaryResponse mapToSummary(Account account) {
		return AccountSummaryResponse.builder().id(account.getId()).accountNumber(account.getAccountNumber())
				.accountType(account.getAccountType()).status(account.getStatus()).balance(account.getBalance())
				.availableBalance(account.getAvailableBalance()).currency(account.getCurrency())
				.isPrimary(account.getIsPrimary()).build();
	}

	private Specification<Account> buildSpecification(AccountFilterRequest filter) {
		// Build JPA Specification for complex filtering
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (filter.getUserId() != null) {
				predicates.add(cb.equal(root.get("userId"), filter.getUserId()));
			}
			if (filter.getAccountTypes() != null) {
				predicates.add(cb.equal(root.get("accountType"), filter.getAccountTypes()));
			}
			if (filter.getStatuses() != null) {
				predicates.add(cb.equal(root.get("status"), filter.getStatuses()));
			}
			if (filter.getMinBalance() != null) {
				predicates.add(cb.greaterThanOrEqualTo(root.get("balance"), filter.getMinBalance()));
			}
			if (filter.getMaxBalance() != null) {
				predicates.add(cb.lessThanOrEqualTo(root.get("balance"), filter.getMaxBalance()));
			}
			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}

	// Circuit breaker fallback
	private AccountResponse getAccountFallback(String accountNumber, Exception e) {
		log.error("Fallback triggered for account: {}", accountNumber, e);
		throw new RuntimeException("Service temporarily unavailable");
	}

	@Override
	@Transactional
	@CacheEvict(value = { "accountDetails", "accountList" }, allEntries = true)
	public AccountResponse freezeAccount(String accountNumber, String reason, String updatedBy) {
		log.info("Freezing account: {}", accountNumber);

		Account account = getAccountEntity(accountNumber);

		if (account.getStatus() == AccountStatus.FROZEN) {
			throw new IllegalStateException("Account is already frozen");
		}

		if (account.getStatus() == AccountStatus.CLOSED) {
			throw new IllegalStateException("Cannot freeze a closed account");
		}

		AccountStatus previousStatus = account.getStatus();
		account.setStatus(AccountStatus.FROZEN);
		account.setFrozenReason(reason);
		account.setUpdatedBy(updatedBy);

		account = accountRepository.save(account);

		// Store event
		AccountStatusChangedEvent event = AccountStatusChangedEvent.builder().accountNumber(accountNumber)
				.previousStatus(previousStatus).newStatus(AccountStatus.FROZEN).reason(reason).build();

		eventSourcingService.storeEvent(accountNumber, "AccountFrozen", event, account.getUserId(),
				UUID.randomUUID().toString(), null);

		outboxService.saveEvent("ACCOUNT", accountNumber, "AccountFrozen", "banking.account.frozen", event);

		log.info("Account frozen: {}", accountNumber);
		return mapToResponse(account);
	}

	@Override
	@Transactional
	@CacheEvict(value = { "accountDetails", "accountList" }, allEntries = true)
	public AccountResponse unfreezeAccount(String accountNumber, String updatedBy) {
		log.info("Unfreezing account: {}", accountNumber);

		Account account = getAccountEntity(accountNumber);

		if (account.getStatus() != AccountStatus.FROZEN) {
			throw new IllegalStateException("Account is not frozen");
		}

		account.setStatus(AccountStatus.ACTIVE);
		account.setFrozenReason(null);
		account.setUpdatedBy(updatedBy);

		account = accountRepository.save(account);

		log.info("Account unfrozen: {}", accountNumber);
		return mapToResponse(account);
	}

	@Override
	@Transactional
	@CacheEvict(value = { "accountDetails", "accountList" }, allEntries = true)
	public AccountResponse activateAccount(String accountNumber, String updatedBy) {
		log.info("Activating account: {}", accountNumber);

		Account account = getAccountEntity(accountNumber);

		if (account.getStatus() == AccountStatus.ACTIVE) {
			throw new IllegalStateException("Account is already active");
		}

		if (account.getStatus() == AccountStatus.CLOSED) {
			throw new IllegalStateException("Cannot activate a closed account");
		}

		account.setStatus(AccountStatus.ACTIVE);
		account.setUpdatedBy(updatedBy);

		account = accountRepository.save(account);

		log.info("Account activated: {}", accountNumber);
		return mapToResponse(account);
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = "primaryAccount", key = "#userId")
	public AccountResponse getPrimaryAccount(Long userId) {
		log.debug("Fetching primary account for user: {}", userId);

		Account account = accountRepository.findByUserIdAndIsPrimaryTrue(userId)
				.orElseThrow(() -> new ResourceNotFoundException("No primary account found for user: " + userId));

		return mapToResponse(account);
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = "userAccountSummary", key = "#userId")
	public UserAccountSummary getUserAccountSummary(Long userId) {
		log.debug("Fetching account summary for user: {}", userId);

		List<Account> accounts = accountRepository.findByUserId(userId);

		if (accounts.isEmpty()) {
			throw new ResourceNotFoundException("No accounts found for user: " + userId);
		}

		Account primaryAccount = accounts.stream().filter(Account::getIsPrimary).findFirst().orElse(null);

		int activeCount = (int) accounts.stream().filter(a -> a.getStatus() == AccountStatus.ACTIVE).count();

		int inactiveCount = (int) accounts.stream()
				.filter(a -> a.getStatus() == AccountStatus.INACTIVE || a.getStatus() == AccountStatus.DORMANT).count();

		int frozenCount = (int) accounts.stream().filter(a -> a.getStatus() == AccountStatus.FROZEN).count();

		BigDecimal totalBalance = accounts.stream().filter(a -> a.getStatus() == AccountStatus.ACTIVE)
				.map(Account::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add);

		BigDecimal totalAvailableBalance = accounts.stream().filter(a -> a.getStatus() == AccountStatus.ACTIVE)
				.map(Account::getAvailableBalance).reduce(BigDecimal.ZERO, BigDecimal::add);

		List<AccountSummaryResponse> accountSummaries = accounts.stream().map(this::mapToSummary)
				.collect(Collectors.toList());

		String userEmail = accounts.get(0).getUserEmail();

		return UserAccountSummary.builder().userId(userId).userEmail(userEmail).totalAccounts(accounts.size())
				.activeAccounts(activeCount).inactiveAccounts(inactiveCount).frozenAccounts(frozenCount)
				.totalBalance(totalBalance).totalAvailableBalance(totalAvailableBalance).accounts(accountSummaries)
				.primaryAccount(primaryAccount != null ? mapToSummary(primaryAccount) : null).build();
	}

	@Override
	@Transactional(readOnly = true)
	public List<AccountEventResponse> getAccountEvents(String accountNumber, Long fromVersion) {
		log.debug("Fetching events for account: {}", accountNumber);

		if (!accountRepository.existsByAccountNumber(accountNumber)) {
			throw new ResourceNotFoundException("Account not found: " + accountNumber);
		}

		List<AccountEventStore> events;

		if (fromVersion != null) {
			events = eventSourcingService.getEventsFromVersion(accountNumber, fromVersion);
		} else {
			events = eventSourcingService.getAccountEvents(accountNumber);
		}

		return events.stream().map(this::mapToEventResponse).collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public AccountResponse getAccountSnapshot(String accountNumber, String pointInTime) {
		log.debug("Fetching snapshot for account: {} at time: {}", accountNumber, pointInTime);

		if (!accountRepository.existsByAccountNumber(accountNumber)) {
			throw new ResourceNotFoundException("Account not found: " + accountNumber);
		}

		try {
			LocalDateTime snapshotTime = LocalDateTime.parse(pointInTime);

			List<AccountEventStore> events = eventSourcingService
					.getEventsByDateRange(accountNumber, LocalDateTime.MIN, snapshotTime);

			if (events.isEmpty()) {
				throw new ResourceNotFoundException("No events found for snapshot");
			}

			Account currentAccount = getAccountEntity(accountNumber);
			return mapToResponse(currentAccount);

		} catch (Exception e) {
			log.error("Failed to get snapshot: {}", e.getMessage());
			throw new RuntimeException("Failed to get account snapshot", e);
		}
	}

	private AccountEventResponse mapToEventResponse(AccountEventStore event) {
		return AccountEventResponse.builder().id(event.getId()).eventId(event.getEventId())
				.aggregateId(event.getAggregateId()).eventType(event.getEventType()).version(event.getVersion())
				.eventData(event.getEventData()).metadata(event.getMetadata()).timestamp(event.getTimestamp()).build();
	}

}
