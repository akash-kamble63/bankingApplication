package com.transaction_service.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.transaction_service.enums.TransactionStatus;
import com.transaction_service.enums.TransactionType;

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
@Table(
    name = "transactions",
    indexes = {
        @Index(name = "idx_transaction_ref", columnList = "transaction_reference", unique = true),
        @Index(name = "idx_source_account", columnList = "source_account_id"),
        @Index(name = "idx_dest_account", columnList = "destination_account_id"),
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_created_at", columnList = "created_at"),
        @Index(name = "idx_idempotency", columnList = "idempotency_key", unique = true),
        @Index(name = "idx_composite", columnList = "user_id, status, created_at") // ✅ Composite for common queries
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "transaction_reference", unique = true, nullable = false, length = 50)
    private String transactionReference; 
    
    @Column(name = "idempotency_key", unique = true, length = 100)
    private String idempotencyKey; 
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "source_account_id", nullable = false)
    private Long sourceAccountId;
    
    @Column(name = "destination_account_id", nullable = false)
    private Long destinationAccountId;
    
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;
    
    @Column(precision = 19, scale = 4)
    private BigDecimal feeAmount = BigDecimal.ZERO; 
    
    @Column(length = 3, nullable = false)
    private String currency = "INR";
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionStatus status;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionType type;
    
    @Column(length = 500)
    private String description;
    
    @Column(name = "hold_reference", length = 50)
    private String holdReference; 
    
    @Column(name = "saga_id", length = 36)
    private String sagaId; 
    
    @Column(name = "correlation_id", length = 36)
    private String correlationId;
    
    @Column(name = "fraud_score", precision = 5, scale = 2)
    private BigDecimal fraudScore;
    
    @Column(name = "fraud_status", length = 20)
    private String fraudStatus; // PENDING, APPROVED, REJECTED
    
    @Column(name = "failure_reason", length = 500)
    private String failureReason;
    
    @Column(name = "processed_by", length = 100)
    private String processedBy;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Version 
    private Long version;
    
   
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;
}
