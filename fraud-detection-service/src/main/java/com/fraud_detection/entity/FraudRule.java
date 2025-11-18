package com.fraud_detection.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "fraud_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudRule {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "rule_name", nullable = false, unique = true)
    private String ruleName;
    
    @Column(name = "rule_type", nullable = false)
    private String ruleType;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "rule_config", columnDefinition = "TEXT")
    private String ruleConfig;
    
    @Column(name = "weight", nullable = false)
    private Double weight;
    
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;
    
    @Column(name = "priority", nullable = false)
    private Integer priority;
    
    @Column(name = "created_at", nullable = false)
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
