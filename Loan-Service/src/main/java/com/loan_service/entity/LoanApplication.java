package com.loan_service.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.loan_service.enums.ApplicationStatus;
import com.loan_service.enums.LoanType;

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
@Table(name = "loan_applications", indexes = {
		@Index(name = "idx_application_number", columnList = "application_number", unique = true),
		@Index(name = "idx_user_id", columnList = "user_id"), @Index(name = "idx_status", columnList = "status") })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplication {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "application_number", unique = true, nullable = false, length = 50)
	private String applicationNumber;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "account_id", nullable = false)
	private Long accountId;

	@Enumerated(EnumType.STRING)
	@Column(name = "loan_type", nullable = false, length = 30)
	private LoanType loanType;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ApplicationStatus status;

	@Column(name = "requested_amount", precision = 19, scale = 4, nullable = false)
	private BigDecimal requestedAmount;

	@Column(name = "requested_tenure_months", nullable = false)
	private Integer requestedTenureMonths;

	@Column(name = "loan_purpose", length = 500)
	private String loanPurpose;

	@Column(name = "annual_income", precision = 19, scale = 4)
	private BigDecimal annualIncome;

	@Column(name = "employment_type", length = 50)
	private String employmentType;

	@Column(name = "company_name", length = 200)
	private String companyName;

	@Column(name = "monthly_obligations", precision = 19, scale = 4)
	private BigDecimal monthlyObligations;

	@Column(name = "collateral_offered")
	private Boolean collateralOffered = false;

	@Column(name = "collateral_details", columnDefinition = "TEXT")
	private String collateralDetails;

	@Column(name = "documents", columnDefinition = "jsonb")
	private String documents;

	@Column(name = "credit_score")
	private Integer creditScore;

	@Column(name = "fraud_score", precision = 5, scale = 2)
	private BigDecimal fraudScore;

	@Column(name = "assigned_to")
	private Long assignedTo;

	@Column(name = "reviewed_by")
	private Long reviewedBy;

	@Column(name = "review_notes", columnDefinition = "TEXT")
	private String reviewNotes;

	@Column(name = "rejection_reason", length = 500)
	private String rejectionReason;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@Version
	private Long version;
}
