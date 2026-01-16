package com.user_service.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Password Reset Token stored in Redis
 * Security enhancements:
 * - Stores HASHED token instead of plain text
 * - Includes IP address and user agent for additional security
 * - Short TTL (15 minutes)
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@RedisHash("password_reset_token")
public class PasswordResetTokenRedis implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * CRITICAL: This is the HASHED token (Hmac-SHA256)
	 * The plain token is NEVER stored, only sent via email
	 */
	@Id
	private String tokenHash;

	/**
	 * Indexed for quick lookup by user
	 */
	@Indexed
	private Long userId;

	private String email;
	private LocalDateTime createdAt;
	private LocalDateTime expiryDate;

	@Builder.Default
	private boolean used = false;

	@Builder.Default
	private int attempts = 0;

	/**
	 * IP address from which the reset was requested
	 * Used for security monitoring and anomaly detection
	 */
	private String ipAddress;

	/**
	 * User agent for additional security context
	 */
	private String userAgent;

	/**
	 * Track when token was actually used (for audit)
	 */
	private LocalDateTime usedAt;
}