package com.account_service.model;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "account_event_store", indexes = {
    @Index(name = "idx_aggregate_id", columnList = "aggregate_id"),
    @Index(name = "idx_aggregate_version", columnList = "aggregate_id, version"),
    @Index(name = "idx_event_type", columnList = "event_type"),
    @Index(name = "idx_timestamp", columnList = "timestamp")
})
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountEventStore {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "event_id", unique = true, nullable = false, length = 36)
    private String eventId; // UUID
    
    @Column(name = "aggregate_id", nullable = false, length = 50)
    private String aggregateId; // Account number
    
    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType; // Always "ACCOUNT"
    
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType; // AccountCreated, BalanceUpdated, etc.
    
    @Column(name = "version", nullable = false)
    private Long version; // Aggregate version (sequential)
    
    @Column(name = "event_data", nullable = false, columnDefinition = "TEXT")
    private String eventData; // JSON event payload
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // Additional context (user, IP, etc.)
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "correlation_id", length = 36)
    private String correlationId; // For tracking related events
    
    @Column(name = "causation_id", length = 36)
    private String causationId; // What caused this event
    
    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;
}
