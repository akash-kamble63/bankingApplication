package com.loan_service.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.loan_service.enums.EmiStatus;

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
@Table(
    name = "emi_schedules",
    indexes = {
        @Index(name = "idx_loan_id", columnList = "loan_id"),
        @Index(name = "idx_due_date", columnList = "due_date"),
        @Index(name = "idx_status", columnList = "status")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmiSchedule {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "loan_id", nullable = false)
    private Long loanId;
    
    @Column(name = "emi_number", nullable = false)
    private Integer emiNumber;
    
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;
    
    @Column(name = "emi_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal emiAmount;
    
    @Column(name = "principal_component", precision = 19, scale = 4, nullable = false)
    private BigDecimal principalComponent;
    
    @Column(name = "interest_component", precision = 19, scale = 4, nullable = false)
    private BigDecimal interestComponent;
    
    @Column(name = "outstanding_principal", precision = 19, scale = 4)
    private BigDecimal outstandingPrincipal;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmiStatus status;
    
    @Column(name = "paid_amount", precision = 19, scale = 4)
    private BigDecimal paidAmount = BigDecimal.ZERO;
    
    @Column(name = "paid_date")
    private LocalDate paidDate;
    
    @Column(name = "payment_reference", length = 100)
    private String paymentReference;
    
    @Column(name = "late_payment_charges", precision = 19, scale = 4)
    private BigDecimal latePaymentCharges = BigDecimal.ZERO;
    
    @Column(name = "bounce_charges", precision = 19, scale = 4)
    private BigDecimal bounceCharges = BigDecimal.ZERO;
    
    @Column(name = "days_overdue")
    private Integer daysOverdue = 0;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Version
    private Long version;
}
