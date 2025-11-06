package com.account_service.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.account_service.enums.TransactionMode;
import com.account_service.enums.TransactionStatus;
import com.account_service.enums.TransactionType;
import com.account_service.enums.TransferMethod;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_account_id", columnList = "account_id"),
    @Index(name = "idx_transaction_ref", columnList = "transaction_reference"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Transaction {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "transaction_reference", unique = true, nullable = false, length = 50)
    private String transactionReference; // Unique transaction ID
    
    @Column(name = "account_id", nullable = false)
    private Long accountId; // Debit/Credit from this account
    
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private TransactionType transactionType;
    
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;
    
    @Column(name = "balance_before", precision = 19, scale = 4)
    private BigDecimal balanceBefore;
    
    @Column(name = "balance_after", precision = 19, scale = 4)
    private BigDecimal balanceAfter;
    
    @Column(length = 3, nullable = false)
    private String currency = "INR";
    
    @Column(name = "from_account_number", length = 16)
    private String fromAccountNumber; // For transfers
    
    @Column(name = "to_account_number", length = 16)
    private String toAccountNumber; // For transfers
    
    @Column(name = "beneficiary_name", length = 100)
    private String beneficiaryName;
    
    @Column(name = "beneficiary_bank", length = 100)
    private String beneficiaryBank;
    
    @Column(name = "beneficiary_ifsc", length = 11)
    private String beneficiaryIfsc;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_mode")
    private TransactionMode transactionMode;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "transfer_method")
    private TransferMethod transferMethod;
    
    @Column(name = "description", length = 255)
    private String description;
    
    @Column(name = "remarks", length = 500)
    private String remarks;
    
    @Column(name = "initiated_by")
    private Long initiatedBy; // User ID who initiated
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(name = "failure_reason")
    private String failureReason;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
