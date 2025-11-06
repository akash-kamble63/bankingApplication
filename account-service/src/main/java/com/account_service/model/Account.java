package com.account_service.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.account_service.enums.AccountStatus;
import com.account_service.enums.AccountType;

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
@Table(name = "accounts", indexes = {
	    @Index(name = "idx_account_number", columnList = "account_number"),
	    @Index(name = "idx_user_id", columnList = "user_id"),
	    @Index(name = "idx_status", columnList = "status")
	})
public class Account {
	 @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Long id;
	    
	    @Column(name = "account_number", unique = true, nullable = false, length = 20)
	    private String accountNumber; // Auto-generated
	    
	    @Column(name = "user_id", nullable = false)
	    private Long userId; // From user-service
	    
	    @Column(name = "user_email", nullable = false)
	    private String userEmail; // Cache for quick lookup
	    
	    @Enumerated(EnumType.STRING)
	    @Column(name = "account_type", nullable = false)
	    private AccountType accountType; // SAVINGS, CURRENT, FIXED_DEPOSIT
	    
	    @Column(nullable = false, precision = 15, scale = 2)
	    private BigDecimal balance = BigDecimal.ZERO;
	    
	    @Column(name = "available_balance", nullable = false, precision = 15, scale = 2)
	    private BigDecimal availableBalance = BigDecimal.ZERO; // Balance - holds
	    
	    @Column(precision = 15, scale = 2)
	    private BigDecimal overdraftLimit = BigDecimal.ZERO; // For current accounts
	    
	    @Enumerated(EnumType.STRING)
	    @Column(nullable = false)
	    private AccountStatus status; // ACTIVE, INACTIVE, FROZEN, CLOSED
	    
	    @Column(length = 3, nullable = false)
	    private String currency = "INR";
	    
	    @Column(name = "branch_code", length = 20)
	    private String branchCode;
	    
	    @Column(name = "ifsc_code", length = 11)
	    private String ifscCode;
	    
	    @Column(name = "is_primary")
	    private Boolean isPrimary = false;
	    
	    @Column(name = "interest_rate", precision = 5, scale = 2)
	    private BigDecimal interestRate; // For savings/FD
	    
	    @Column(name = "maturity_date")
	    private LocalDateTime maturityDate; // For FD
	    
	    @Column(name = "min_balance", precision = 15, scale = 2)
	    private BigDecimal minimumBalance = BigDecimal.ZERO;
	    
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
	    private Long version; // Optimistic locking
}
