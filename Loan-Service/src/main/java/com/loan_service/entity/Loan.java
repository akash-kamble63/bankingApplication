package com.loan_service.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

import com.loan_service.enums.InterestType;
import com.loan_service.enums.LoanStatus;
import com.loan_service.enums.LoanType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "loans",
    indexes = {
        @Index(name = "idx_loan_number", columnList = "loan_number", unique = true),
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_account_id", columnList = "account_id"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_loan_type", columnList = "loan_type"),
        @Index(name = "idx_composite", columnList = "user_id, status, created_at DESC")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Loan {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "loan_number", unique = true, nullable = false, length = 50)
    private String loanNumber; // LOAN-XXX-XXX
    
    @Column(name = "application_id", nullable = false)
    private Long applicationId;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "account_id", nullable = false)
    private Long accountId; 
    
    @Enumerated(EnumType.STRING)
    @Column(name = "loan_type", nullable = false, length = 30)
    private LoanType loanType;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LoanStatus status;
    
    // Loan amounts
    @Column(name = "principal_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal principalAmount;
    
    @Column(name = "sanctioned_amount", precision = 19, scale = 4)
    private BigDecimal sanctionedAmount;
    
    @Column(name = "disbursed_amount", precision = 19, scale = 4)
    private BigDecimal disbursedAmount;
    
    @Column(name = "outstanding_principal", precision = 19, scale = 4)
    private BigDecimal outstandingPrincipal;
    
    @Column(name = "outstanding_interest", precision = 19, scale = 4)
    private BigDecimal outstandingInterest = BigDecimal.ZERO;
    
    @Column(name = "total_outstanding", precision = 19, scale = 4)
    private BigDecimal totalOutstanding;
    
    @Column(name = "total_paid", precision = 19, scale = 4)
    private BigDecimal totalPaid = BigDecimal.ZERO;
    
    // Interest & Tenure
    @Column(name = "interest_rate", precision = 5, scale = 2, nullable = false)
    private BigDecimal interestRate; // Annual percentage rate
    
    @Column(name = "tenure_months", nullable = false)
    private Integer tenureMonths;
    
    @Column(name = "remaining_tenure_months")
    private Integer remainingTenureMonths;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "interest_type", length = 30)
    private InterestType interestType; 
    
    // EMI Details
    @Column(name = "emi_amount", precision = 19, scale = 4)
    private BigDecimal emiAmount;
    
    @Column(name = "emi_day", nullable = false)
    private Integer emiDay; // Day of month (1-28)
    
    @Column(name = "first_emi_date")
    private LocalDate firstEmiDate;
    
    @Column(name = "next_emi_date")
    private LocalDate nextEmiDate;
    
    @Column(name = "last_emi_date")
    private LocalDate lastEmiDate;
    
    @Column(name = "total_emis")
    private Integer totalEmis;
    
    @Column(name = "paid_emis")
    private Integer paidEmis = 0;
    
    @Column(name = "missed_emis")
    private Integer missedEmis = 0;
    
    @Column(name = "prepayment_allowed")
    private Boolean prepaymentAllowed = true;
    
    @Column(name = "prepayment_charges_percentage", precision = 5, scale = 2)
    private BigDecimal prepaymentChargesPercentage;
    
    // Fees & Charges
    @Column(name = "processing_fee", precision = 19, scale = 4)
    private BigDecimal processingFee;
    
    @Column(name = "processing_fee_paid")
    private Boolean processingFeePaid = false;
    
    @Column(name = "late_payment_penalty_rate", precision = 5, scale = 2)
    private BigDecimal latePaymentPenaltyRate;
    
    @Column(name = "total_late_payment_charges", precision = 19, scale = 4)
    private BigDecimal totalLatePaymentCharges = BigDecimal.ZERO;
    
    @Column(name = "bounce_charges", precision = 19, scale = 4)
    private BigDecimal bounceCharges;
    
    // Purpose & Documents
    @Column(name = "loan_purpose", length = 500)
    private String loanPurpose;
    
    @Column(name = "collateral_type", length = 100)
    private String collateralType;
    
    @Column(name = "collateral_value", precision = 19, scale = 4)
    private BigDecimal collateralValue;
    
    @Column(name = "collateral_description", columnDefinition = "TEXT")
    private String collateralDescription;
    
    @Column(name = "documents", columnDefinition = "jsonb")
    private String documents; 
    
    // Credit Information
    @Column(name = "credit_score")
    private Integer creditScore;
    
    @Column(name = "credit_bureau", length = 50)
    private String creditBureau;
    
    @Column(name = "debt_to_income_ratio", precision = 5, scale = 2)
    private BigDecimal debtToIncomeRatio;
    
    @Column(name = "annual_income", precision = 19, scale = 4)
    private BigDecimal annualIncome;
    
    @Column(name = "employment_type", length = 50)
    private String employmentType; 
    
    // Guarantor/Co-Applicant
    @Column(name = "guarantor_id")
    private Long guarantorId;
    
    @Column(name = "guarantor_name", length = 100)
    private String guarantorName;
    
    @Column(name = "guarantor_relationship", length = 50)
    private String guarantorRelationship;
    
    @Column(name = "co_applicant_id")
    private Long coApplicantId;
    
    // Insurance
    @Column(name = "insurance_enabled")
    private Boolean insuranceEnabled = false;
    
    @Column(name = "insurance_premium", precision = 19, scale = 4)
    private BigDecimal insurancePremium;
    
    @Column(name = "insurance_policy_number", length = 100)
    private String insurancePolicyNumber;
    
    // Moratorium
    @Column(name = "moratorium_months")
    private Integer moratoriumMonths = 0;
    
    @Column(name = "moratorium_end_date")
    private LocalDate moratoriumEndDate;
    
    // Dates
    @Column(name = "application_date")
    private LocalDate applicationDate;
    
    @Column(name = "approval_date")
    private LocalDate approvalDate;
    
    @Column(name = "disbursement_date")
    private LocalDate disbursementDate;
    
    @Column(name = "maturity_date")
    private LocalDate maturityDate;
    
    @Column(name = "closed_date")
    private LocalDate closedDate;
    
    // Approval Details
    @Column(name = "approved_by")
    private Long approvedBy;
    
    @Column(name = "approval_notes", columnDefinition = "TEXT")
    private String approvalNotes;
    
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;
    
    // References
    @Column(name = "previous_loan_id")
    private Long previousLoanId; 
    
    @Column(name = "parent_loan_id")
    private Long parentLoanId; 
    
    // Metadata
    @Column(columnDefinition = "jsonb")
    private String metadata;
    
    @Column(name = "correlation_id", length = 36)
    private String correlationId;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Version
    private Long version;
}
