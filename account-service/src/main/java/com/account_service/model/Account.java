package com.account_service.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.account_service.enums.AccountHolderType;
import com.account_service.enums.AccountStatus;
import com.account_service.enums.AccountType;
import com.account_service.enums.InterestFrequency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "accounts", indexes = { @Index(name = "idx_account_number", columnList = "account_number"),
		@Index(name = "idx_user_id", columnList = "user_id"), @Index(name = "idx_status", columnList = "status"),
		@Index(name = "idx_created_at", columnList = "created_at") // Add this for reporting
})
public class Account {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "account_number", unique = true, nullable = false, length = 20)
	private String accountNumber;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "user_email", nullable = false)
	private String userEmail;

	@Enumerated(EnumType.STRING)
	@Column(name = "account_type", nullable = false)
	private AccountType accountType;

	@Column(nullable = false, precision = 15, scale = 2)
	private BigDecimal balance = BigDecimal.ZERO;

	@Column(name = "available_balance", nullable = false, precision = 15, scale = 2)
	private BigDecimal availableBalance = BigDecimal.ZERO;

	@Column(precision = 15, scale = 2)
	private BigDecimal overdraftLimit = BigDecimal.ZERO;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AccountStatus status;

	@Enumerated(EnumType.STRING)
	@Column(name = "holder_type", nullable = false)
	private AccountHolderType holderType;

	@Enumerated(EnumType.STRING)
	@Column(name = "interest_frequency")
	private InterestFrequency interestFrequency;

	@Column(length = 3, nullable = false)
	private String currency = "INR";

	@Column(name = "branch_code", length = 20)
	private String branchCode;

	@Column(name = "ifsc_code", length = 11)
	private String ifscCode;

	@Column(name = "is_primary")
	private Boolean isPrimary = false;

	@Column(name = "interest_rate", precision = 5, scale = 2)
	private BigDecimal interestRate;

	@Column(name = "maturity_date")
	private LocalDateTime maturityDate;

	@Column(name = "min_balance", precision = 15, scale = 2)
	private BigDecimal minimumBalance = BigDecimal.ZERO;

	// ADD THESE FIELDS:

	@Column(name = "daily_transaction_limit", precision = 15, scale = 2)
	private BigDecimal dailyTransactionLimit = new BigDecimal("100000"); // 1 lakh default

	@Column(name = "daily_withdrawal_limit", precision = 15, scale = 2)
	private BigDecimal dailyWithdrawalLimit = new BigDecimal("50000");

	@Column(name = "monthly_transaction_limit", precision = 15, scale = 2)
	private BigDecimal monthlyTransactionLimit = new BigDecimal("500000");

	@Column(name = "total_credited", precision = 15, scale = 2)
	private BigDecimal totalCredited = BigDecimal.ZERO; // Lifetime credits

	@Column(name = "total_debited", precision = 15, scale = 2)
	private BigDecimal totalDebited = BigDecimal.ZERO; // Lifetime debits

	@Column(name = "transaction_count")
	private Long transactionCount = 0L;

	@Column(name = "frozen_reason", length = 500)
	private String frozenReason; // Why account was frozen

	@Column(name = "closure_reason", length = 500)
	private String closureReason; // Why account was closed

	@Column(name = "created_by")
	private String createdBy; // Admin/System username

	@Column(name = "updated_by")
	private String updatedBy;

	@Column(name = "last_transaction_date")
	private LocalDateTime lastTransactionDate;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@Column(name = "closed_at")
	private LocalDateTime closedAt;

	@Version
	private Long version;
}
