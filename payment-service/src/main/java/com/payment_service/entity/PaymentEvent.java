package com.payment_service.entity;

import java.time.LocalDateTime;

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
@Table(name = "payment_events", indexes = {
    @Index(name = "idx_aggregate_id", columnList = "aggregate_id"),
    @Index(name = "idx_event_type", columnList = "event_type"),
    @Index(name = "idx_occurred_at", columnList = "occurred_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "aggregate_id", nullable = false, length = 50)
    private String aggregateId; // Payment reference
    
    @Column(name = "sequence", nullable = false)
    private Long sequence;
    
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;
    
    @Column(name = "event_data", columnDefinition = "TEXT", nullable = false)
    private String eventData; // JSON
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "correlation_id", length = 36)
    private String correlationId;
    
    @Column(name = "causation_id", length = 36)
    private String causationId;
    
    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;
    
    @Column(columnDefinition = "jsonb")
    private String metadata;
}
