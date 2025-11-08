package com.account_service.model;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
	    name = "idempotency_records",
	    indexes = {
	        @Index(name = "idx_idempotency_key", columnList = "idempotency_key", unique = true),
	        @Index(name = "idx_idempotency_created_at", columnList = "created_at")
	    })
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IdempotencyRecord {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "idempotency_key", unique = true, nullable = false, length = 100)
    private String idempotencyKey;
    
    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash; // SHA-256 of request body
    
    @Column(name = "response_status")
    private Integer responseStatus;
    
    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;
    
    @Column(name = "endpoint", length = 255)
    private String endpoint;
    
    @Column(name = "http_method", length = 10)
    private String httpMethod;
    
    @Column(name = "user_id")
    private Long userId;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(name = "processing")
    private Boolean processing = false; // To handle concurrent requests
    
    @Version
    private Long version;
}
