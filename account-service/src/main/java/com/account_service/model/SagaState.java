package com.account_service.model;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
	    private String sagaId; // UUID
	    
	    @Column(name = "saga_type", nullable = false, length = 50)
	    private String sagaType; // FUND_TRANSFER, ACCOUNT_CREATION, etc.
	    
	    @Column(name = "status", nullable = false, length = 30)
	    @Enumerated(EnumType.STRING)
	    private SagaStatus status;
	    
	    @Column(name = "current_step", length = 50)
	    private String currentStep;
	    
	    @Column(name = "completed_steps", columnDefinition = "TEXT")
	    private String completedSteps; // JSON array of completed steps
	    
	    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
	    private String payload; // JSON saga data
	    
	    @Column(name = "compensation_data", columnDefinition = "TEXT")
	    private String compensationData; // Data needed for rollback
	    
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
	    
	    public enum SagaStatus {
	        STARTED,
	        PROCESSING,
	        COMPENSATING,
	        COMPLETED,
	        FAILED,
	        COMPENSATED
	    }
}
