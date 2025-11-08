package com.account_service.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

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
    private String aggregateType; // e.g., "ACCOUNT", "TRANSACTION"
    
    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId; // e.g., account number
    
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType; // e.g., "AccountCreated", "BalanceUpdated"
    
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
    
    public enum OutboxStatus {
        PENDING,
        PUBLISHED,
        FAILED
    }
    
    public void incrementRetry() {
        this.retryCount++;
        this.nextRetryAt = LocalDateTime.now().plusMinutes(retryCount * 5L); // Exponential backoff
    }
}