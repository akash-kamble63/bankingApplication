package com.transaction_service.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "transaction_event_store", indexes = {
    @Index(name = "idx_aggregate_id", columnList = "aggregate_id"),
    @Index(name = "idx_aggregate_version", columnList = "aggregate_id, version"),
    @Index(name = "idx_event_type", columnList = "event_type")
})
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionEventStore {
	 @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Long id;
	    
	    @Column(name = "event_id", unique = true, nullable = false, length = 36)
	    private String eventId;
	    
	    @Column(name = "aggregate_id", nullable = false, length = 50)
	    private String aggregateId;
	    
	    @Column(name = "aggregate_type", nullable = false, length = 50)
	    private String aggregateType;
	    
	    @Column(name = "event_type", nullable = false, length = 100)
	    private String eventType;
	    
	    @Column(name = "version", nullable = false)
	    private Long version;
	    
	    @Column(name = "event_data", nullable = false, columnDefinition = "TEXT")
	    private String eventData;
	    
	    @Column(name = "metadata", columnDefinition = "TEXT")
	    private String metadata;
	    
	    @Column(name = "user_id")
	    private Long userId;
	    
	    @Column(name = "correlation_id", length = 36)
	    private String correlationId;
	    
	    @Column(name = "causation_id", length = 36)
	    private String causationId;
	    
	    @CreationTimestamp
	    @Column(name = "timestamp", nullable = false, updatable = false)
	    private LocalDateTime timestamp;
}
