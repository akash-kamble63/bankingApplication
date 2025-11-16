package com.notification.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.notification.enums.NotificationChannel;
import com.notification.enums.NotificationPriority;
import com.notification.enums.NotificationStatus;
import com.notification.enums.NotificationType;

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
@Table(name = "notifications", indexes = { @Index(name = "idx_user_id", columnList = "user_id"),
		@Index(name = "idx_status", columnList = "status"), @Index(name = "idx_channel", columnList = "channel"),
		@Index(name = "idx_scheduled_at", columnList = "scheduled_at"),
		@Index(name = "idx_composite", columnList = "user_id, status, created_at DESC"),
		@Index(name = "idx_reference", columnList = "reference_id") })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "reference_id", length = 100)
	private String referenceId; // Payment ref, transaction ref, etc.

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private NotificationType type;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private NotificationChannel channel; // EMAIL, SMS, PUSH, IN_APP

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private NotificationPriority priority;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private NotificationStatus status;

	@Column(nullable = false, length = 200)
	private String subject;

	@Column(columnDefinition = "TEXT", nullable = false)
	private String content;

	@Column(name = "template_id", length = 50)
	private String templateId;

	@Column(name = "template_data", columnDefinition = "jsonb")
	private String templateData; // JSON data for template variables

	// Channel specific fields
	@Column(name = "recipient_email", length = 255)
	private String recipientEmail;

	@Column(name = "recipient_phone", length = 20)
	private String recipientPhone;

	@Column(name = "device_token", length = 500)
	private String deviceToken; // For push notifications

	// Delivery tracking
	@Column(name = "sent_at")
	private LocalDateTime sentAt;

	@Column(name = "delivered_at")
	private LocalDateTime deliveredAt;

	@Column(name = "read_at")
	private LocalDateTime readAt;

	@Column(name = "scheduled_at")
	private LocalDateTime scheduledAt;

	@Column(name = "expires_at")
	private LocalDateTime expiresAt;

	// Error tracking
	@Column(name = "retry_count")
	private Integer retryCount = 0;

	@Column(name = "max_retries")
	private Integer maxRetries = 3;

	@Column(name = "error_message", length = 1000)
	private String errorMessage;

	@Column(name = "external_id", length = 100)
	private String externalId; // Third-party provider ID

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
