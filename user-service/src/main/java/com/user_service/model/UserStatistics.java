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
@Table(name = "user_statistics")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserStatistics {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", unique = true, nullable = false)
	private Long userId;

	@Column(name = "total_logins")
	private Integer totalLogins = 0;

	@Column(name = "last_login_at")
	private LocalDateTime lastLoginAt;

	@Column(name = "total_password_changes")
	private Integer totalPasswordChanges = 0;

	@Column(name = "total_profile_updates")
	private Integer totalProfileUpdates = 0;

	@Column(name = "total_failed_logins")
	private Integer totalFailedLogins = 0;

	@Column(name = "account_created_at")
	private LocalDateTime accountCreatedAt;

	@Column(name = "last_activity_at")
	private LocalDateTime lastActivityAt;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;
}