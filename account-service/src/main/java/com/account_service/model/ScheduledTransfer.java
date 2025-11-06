package com.account_service.model;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.account_service.enums.ScheduledTransferFrequency;
import com.account_service.enums.ScheduledTransferStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Entity
@Table(name = "scheduled_transfers")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ScheduledTransfer {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "from_account_id", nullable = false)
    private Long fromAccountId;
    
    @Column(name = "to_account_number", nullable = false, length = 16)
    private String toAccountNumber;
    
    @Column(name = "beneficiary_id")
    private Long beneficiaryId;
    
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScheduledTransferFrequency frequency; // ONCE, DAILY, WEEKLY, MONTHLY
    
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    
    @Column(name = "end_date")
    private LocalDate endDate;
    
    @Column(name = "next_execution_date")
    private LocalDate nextExecutionDate;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScheduledTransferStatus status;
    
    @Column(name = "description", length = 255)
    private String description;
    
    @Column(name = "execution_count")
    private Integer executionCount = 0;
    
    @Column(name = "last_executed_at")
    private LocalDateTime lastExecutedAt;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
