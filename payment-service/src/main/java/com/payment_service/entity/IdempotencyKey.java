package com.payment_service.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.payment_service.enums.IdempotencyStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Idempotency tracking entity.
 * Stores idempotency keys to prevent duplicate payment processing.
 * Keys expire after 24 hours as per industry standards.
 */
@Entity
@Table(name = "idempotency_keys", indexes = {
        @Index(name = "idx_idem_key", columnList = "idempotency_key", unique = true),
        @Index(name = "idx_idem_user_created", columnList = "user_id, created_at"),
        @Index(name = "idx_idem_status", columnList = "status"),
        @Index(name = "idx_idem_expires", columnList = "expires_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyKey {

    @Id
    @Column(name = "idempotency_key", length = 100, nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "request_path", nullable = false, length = 255)
    private String requestPath; // e.g., /api/v1/payments/card

    @Column(name = "request_method", nullable = false, length = 10)
    private String requestMethod; // POST, PUT, etc.

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash; // SHA-256 hash of request body

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IdempotencyStatus status;

    @Column(name = "payment_reference", length = 50)
    private String paymentReference; // Link to created payment

    @Column(name = "response_code")
    private Integer responseCode; // HTTP status code

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody; // Cached response

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt; // Auto-expire after 24 hours

    @Column(name = "request_ip", length = 45)
    private String requestIp;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * Check if this idempotency key has expired
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Check if the request is still processing
     */
    public boolean isProcessing() {
        return status == IdempotencyStatus.PROCESSING;
    }

    /**
     * Check if the request has been completed
     */
    public boolean isCompleted() {
        return status == IdempotencyStatus.COMPLETED;
    }

    /**
     * Check if the request has failed
     */
    public boolean isFailed() {
        return status == IdempotencyStatus.FAILED;
    }
}