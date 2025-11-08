package com.account_service.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.account_service.enums.HoldStatus;

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
@Table(name = "account_holds", indexes = {
    @Index(name = "idx_account_id", columnList = "account_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_expires_at", columnList = "expires_at")
})
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountHold {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "hold_reference", unique = true, nullable = false)
    private String holdReference; // Unique hold ID
    
    @Column(name = "account_id", nullable = false)
    private Long accountId;
    
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HoldStatus status; // ACTIVE, RELEASED, EXPIRED
    
    @Column(length = 255)
    private String reason; // Why hold placed
    
    @Column(name = "transaction_ref", length = 50)
    private String transactionReference; // Link to transaction
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt; // Auto-release after this time
    
    @Column(name = "released_at")
    private LocalDateTime releasedAt;
}
