package com.payment_service.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.payment_service.enums.SagaStatus;

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
@Table(name = "saga_states", indexes = {
    @Index(name = "idx_saga_id", columnList = "saga_id", unique = true),
    @Index(name = "idx_saga_status", columnList = "status"),
    @Index(name = "idx_saga_type", columnList = "saga_type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaState {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "saga_id", unique = true, nullable = false, length = 36)
    private String sagaId;
    
    @Column(name = "saga_type", nullable = false, length = 50)
    private String sagaType; // CARD_PAYMENT, UPI_PAYMENT, BILL_PAYMENT
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SagaStatus status;
    
    @Column(name = "current_step", length = 50)
    private String currentStep;
    
    @Column(columnDefinition = "TEXT")
    private String payload; // JSON of PaymentSagaData
    
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    
    @Column(name = "retry_count")
    private Integer retryCount = 0;
    
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
