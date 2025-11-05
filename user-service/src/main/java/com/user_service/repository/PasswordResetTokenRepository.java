package com.user_service.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.user_service.model.PasswordResetToken;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
	Optional<PasswordResetToken> findByToken(String token);

	Optional<PasswordResetToken> findByTokenAndUsedFalse(String token);

	@Modifying
	@Query("DELETE FROM PasswordResetToken p WHERE p.expiryDate < ?1")
	void deleteExpiredTokens(LocalDateTime now);

	@Modifying
	@Query("DELETE FROM PasswordResetToken p WHERE p.user.id = ?1")
	void deleteByUserId(Long userId);
}
