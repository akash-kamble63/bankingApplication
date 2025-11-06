package com.account_service.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.account_service.enums.StatementFormat;
import com.account_service.enums.StatementStatus;

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
@Table(name = "account_statements")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountStatement {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "account_id", nullable = false)
	private Long accountId;

	@Column(name = "from_date", nullable = false)
	private LocalDate fromDate;

	@Column(name = "to_date", nullable = false)
	private LocalDate toDate;

	@Column(name = "file_path")
	private String filePath; // S3 URL or local path

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private StatementFormat format; // PDF, CSV, EXCEL

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private StatementStatus status; // GENERATING, READY, FAILED

	@CreationTimestamp
	@Column(name = "generated_at")
	private LocalDateTime generatedAt;

	@Column(name = "expires_at")
	private LocalDateTime expiresAt;
	
}
