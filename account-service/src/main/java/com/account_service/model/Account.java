package com.account_service.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.account_service.enums.AccountHolderType;
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
@Table(name = "accounts", indexes = { @Index(name = "idx_account_number", columnList = "account_number"),
		@Index(name = "idx_user_id", columnList = "user_id"), @Index(name = "idx_status", columnList = "status"),
		@Index(name = "idx_created_at", columnList = "created_at") // Add this for reporting
})
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    

    @Column(name = "account_number", unique = true, nullable = false, length = 16)
    private String accountNumber; 
    
    @Column(name = "user_id", nullable = false)
    private Long userId; 
    
    @Column(name = "user_email", nullable = false, length = 100)
    private String userEmail; 
    

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 30)
    private AccountType accountType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 20)
    private AccountStatus status; 
    
    @Enumerated(EnumType.STRING)
    @Column(name = "holder_type", nullable = false, length = 20)
    private AccountHolderType holderType; 
    

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance = BigDecimal.ZERO;
    
    @Column(name = "available_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal availableBalance = BigDecimal.ZERO;
    
    @Column(name = "minimum_balance", precision = 19, scale = 4)
    private BigDecimal minimumBalance = BigDecimal.ZERO;
    
    @Column(name = "overdraft_limit", precision = 19, scale = 4)
    private BigDecimal overdraftLimit = BigDecimal.ZERO; 
    

    @Column(length = 3, nullable = false)
    private String currency = "INR";
    
    @Column(name = "branch_code", length = 10)
    private String branchCode; 
    
    @Column(name = "ifsc_code", length = 11)
    private String ifscCode; 
    

    @Column(name = "is_primary")
    private Boolean isPrimary = false; 
    

    @Column(name = "interest_rate", precision = 5, scale = 2)
    private BigDecimal interestRate; // e.g., 4.5% = 4.50
    
    @Column(name = "maturity_date")
    private LocalDateTime maturityDate; 
    

    @Column(name = "frozen_reason", length = 500)
    private String frozenReason;
    
    @Column(name = "closure_reason", length = 500)
    private String closureReason;
    

    @Column(name = "created_by", length = 100)
    private String createdBy; 
    
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
    
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
