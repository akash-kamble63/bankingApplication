package com.user_service.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notification_preferences")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationPreferences {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", unique = true, nullable = false)
	private Long userId;

	// Channel preferences
	@Column(name = "email_enabled")
	private Boolean emailEnabled = true;

	@Column(name = "sms_enabled")
	private Boolean smsEnabled = false;

	@Column(name = "push_enabled")
	private Boolean pushEnabled = true;

	// Event preferences
	@Column(name = "email_on_login")
	private Boolean emailOnLogin = true;

	@Column(name = "email_on_password_change")
	private Boolean emailOnPasswordChange = true;

	@Column(name = "email_on_profile_update")
	private Boolean emailOnProfileUpdate = false;

	@Column(name = "email_on_account_activity")
	private Boolean emailOnAccountActivity = true;

	@Column(name = "email_marketing")
	private Boolean emailMarketing = false;

	@Column(name = "email_security_alerts")
	private Boolean emailSecurityAlerts = true;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;
}