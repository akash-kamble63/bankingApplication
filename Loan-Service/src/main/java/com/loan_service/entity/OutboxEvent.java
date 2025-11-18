package com.loan_service.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.loan_service.enums.OutboxStatus;

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
@Table(name = "outbox_events", indexes = {
    @Index(name = "idx_status_created", columnList = "status, created_at"),
    @Index(name = "idx_aggregate", columnList = "aggregate_type, aggregate_id")
})
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OutboxEvent {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "event_id", unique = true, nullable = false, length = 36)
    private String eventId; // UUID
    
    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType; // e.g., "LOAN", "LOAN_APPLICATION"
    
    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId; // e.g., loan number
    
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType; // e.g., "LoanApproved", "LoanDisbursed"
    
    @Column(name = "topic", nullable = false, length = 100)
    private String topic; // Kafka topic
    
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload; // JSON payload
    
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private OutboxStatus status; // PENDING, PUBLISHED, FAILED
    
    @Column(name = "retry_count")
    private Integer retryCount = 0;
    
    @Column(name = "max_retries")
    private Integer maxRetries = 3;
    
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "published_at")
    private LocalDateTime publishedAt;
    
    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;
    
    @Version
    private Long version;
    
    public void incrementRetry() {
        this.retryCount++;
        this.nextRetryAt = LocalDateTime.now().plusMinutes(retryCount * 5L); // Exponential backoff
    }
}
