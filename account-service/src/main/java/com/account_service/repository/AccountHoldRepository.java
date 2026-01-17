package com.account_service.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.account_service.enums.HoldStatus;
import com.account_service.model.AccountHold;

public interface AccountHoldRepository extends JpaRepository<AccountHold, Long> {

	/**
	 * Find all unreleased holds for an account
	 */
	List<AccountHold> findByAccountIdAndReleasedFalse(Long accountId);

	// Find operations
	Optional<AccountHold> findByHoldReference(String holdReference);

	List<AccountHold> findByAccountId(Long accountId);

	List<AccountHold> findByAccountIdAndStatus(Long accountId, HoldStatus status);

	// Active holds
	@Query("SELECT h FROM AccountHold h WHERE h.accountId = :accountId AND h.status = 'ACTIVE' AND h.expiresAt > CURRENT_TIMESTAMP")
	List<AccountHold> findActiveHoldsByAccountId(@Param("accountId") Long accountId);

	// Sum of holds
	@Query("SELECT COALESCE(SUM(h.amount), 0) FROM AccountHold h WHERE h.accountId = :accountId AND h.status = 'ACTIVE'")
	BigDecimal sumActiveHoldsByAccountId(@Param("accountId") Long accountId);

	// Expired holds
	@Query("SELECT h FROM AccountHold h WHERE h.status = 'ACTIVE' AND h.expiresAt < :expiryTime")
	List<AccountHold> findExpiredHolds(@Param("expiryTime") LocalDateTime expiryTime);

	long countByAccountIdAndReleasedFalse(Long accountId);

	List<AccountHold> findByTransactionReference(String transactionReference);

	// Update operations
	@Modifying
	@Query("UPDATE AccountHold h SET h.status = :status, h.releasedAt = :releasedAt WHERE h.id = :holdId")
	int releaseHold(@Param("holdId") Long holdId, @Param("status") HoldStatus status,
			@Param("releasedAt") LocalDateTime releasedAt);

	@Modifying
	@Query("UPDATE AccountHold h SET h.status = 'EXPIRED' WHERE h.status = 'ACTIVE' AND h.expiresAt < :currentTime")
	int expireHolds(@Param("currentTime") LocalDateTime currentTime);

	// Count operations
	long countByAccountIdAndStatus(Long accountId, HoldStatus status);

	@Modifying
	@Query("UPDATE AccountHold h SET h.released = true, h.releasedAt = :now " +
			"WHERE h.released = false AND h.expiresAt IS NOT NULL AND h.expiresAt < :now")
	int autoReleaseExpiredHolds(@Param("now") LocalDateTime now);
}