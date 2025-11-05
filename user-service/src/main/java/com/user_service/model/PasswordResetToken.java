package com.user_service.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Entity
@Table(name = "password_reset_tokens")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PasswordResetToken {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String token;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "expiry_date", nullable = false)
	private LocalDateTime expiryDate;

	@Column(nullable = false)
	private Boolean used = false;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	public boolean isExpired() {
		return LocalDateTime.now().isAfter(expiryDate);
	}
}
