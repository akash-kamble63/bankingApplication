package com.notification.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notification_preferences", indexes = {
		@Index(name = "idx_user_id", columnList = "user_id", unique = true) })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreference {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", unique = true, nullable = false)
	private Long userId;

	// Channel preferences
	@Column(name = "email_enabled")
	private Boolean emailEnabled = true;

	@Column(name = "sms_enabled")
	private Boolean smsEnabled = true;

	@Column(name = "push_enabled")
	private Boolean pushEnabled = true;

	@Column(name = "in_app_enabled")
	private Boolean inAppEnabled = true;

	// Type preferences
	@Column(name = "transaction_alerts")
	private Boolean transactionAlerts = true;

	@Column(name = "payment_alerts")
	private Boolean paymentAlerts = true;

	@Column(name = "security_alerts")
	private Boolean securityAlerts = true;

	@Column(name = "marketing_alerts")
	private Boolean marketingAlerts = false;

	@Column(name = "promotional_alerts")
	private Boolean promotionalAlerts = false;

	// Quiet hours
	@Column(name = "quiet_hours_enabled")
	private Boolean quietHoursEnabled = false;

	@Column(name = "quiet_hours_start")
	private String quietHoursStart; // "22:00"

	@Column(name = "quiet_hours_end")
	private String quietHoursEnd; // "08:00"

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;
}
