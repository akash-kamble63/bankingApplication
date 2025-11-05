package com.user_service.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PasswordResetTokenRedis implements Serializable {
	private static final long serialVersionUID = 1L;

	private String token;
	private Long userId;
	private String email;
	private LocalDateTime createdAt;
	private LocalDateTime expiryDate;
	private boolean used;
	private int attempts; // Track reset attempts
}
