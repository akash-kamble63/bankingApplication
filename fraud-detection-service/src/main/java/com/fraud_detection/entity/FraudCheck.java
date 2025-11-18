package com.fraud_detection.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fraud_detection.enums.FraudStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "fraud_checks", indexes = {
	    @Index(name = "idx_transaction_id", columnList = "transaction_id"),
	    @Index(name = "idx_account_id", columnList = "account_id"),
	    @Index(name = "idx_status", columnList = "status"),
	    @Index(name = "idx_created_at", columnList = "created_at")})
@Data
public class FraudCheck {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "transaction_id", nullable = false, unique = true)
    private String transactionId;
    
    @Column(name = "account_id", nullable = false)
    private String accountId;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Column(name = "amount", nullable = false)
    private BigDecimal amount;
    
    @Column(name = "currency", nullable = false)
    private String currency;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FraudStatus status;
    
    @Column(name = "risk_score", nullable = false)
    private Double riskScore;
    
    @Column(name = "fraud_reasons", columnDefinition = "TEXT")
    private String fraudReasons;
    
    @Column(name = "merchant_name")
    private String merchantName;
    
    @Column(name = "merchant_category")
    private String merchantCategory;
    
    @Column(name = "transaction_type")
    private String transactionType;
    
    @Column(name = "location_country")
    private String locationCountry;
    
    @Column(name = "location_city")
    private String locationCity;
    
    @Column(name = "latitude")
    private Double latitude;
    
    @Column(name = "longitude")
    private Double longitude;
    
    @Column(name = "device_id")
    private String deviceId;
    
    @Column(name = "ip_address")
    private String ipAddress;
    
    @Column(name = "reviewed", nullable = false)
    private Boolean reviewed = false;
    
    @Column(name = "reviewed_by")
    private String reviewedBy;
    
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
    
    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
