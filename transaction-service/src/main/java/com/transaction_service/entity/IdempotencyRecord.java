package com.transaction_service.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "idempotency_records", indexes = {
		@Index(name = "idx_idempotency_key", columnList = "idempotency_key", unique = true) })
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
	private String requestHash;

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
	private Boolean processing = false;

	@Version
	private Long version;
}
