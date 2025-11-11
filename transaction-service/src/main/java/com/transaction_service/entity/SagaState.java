package com.transaction_service.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.transaction_service.enums.SagaStatus;

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
@Table(name = "saga_state", indexes = {
    @Index(name = "idx_saga_id", columnList = "saga_id", unique = true),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_saga_type", columnList = "saga_type")
})
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SagaState {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "saga_id", unique = true, nullable = false, length = 36)
    private String sagaId;
    
    @Column(name = "saga_type", nullable = false, length = 50)
    private String sagaType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SagaStatus status;
    
    @Column(name = "current_step", length = 50)
    private String currentStep;
    
    @Column(name = "completed_steps", columnDefinition = "TEXT")
    private String completedSteps;
    
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;
    
    @Column(name = "compensation_data", columnDefinition = "TEXT")
    private String compensationData;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "retry_count")
    private Integer retryCount = 0;
    
    @Column(name = "max_retries")
    private Integer maxRetries = 3;
    
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
}
