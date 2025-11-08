package com.account_service.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.account_service.enums.AccountStatus;
import com.account_service.enums.AccountType;
import com.account_service.model.Account;

import jakarta.persistence.LockModeType;

public interface AccountRepository extends JpaRepository<Account, Long>, JpaSpecificationExecutor<Account> {

	// Find operations
	Optional<Account> findByAccountNumber(String accountNumber);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber")
	Optional<Account> findByAccountNumberForUpdate(@Param("accountNumber") String accountNumber);

	List<Account> findByStatus(AccountStatus status);
	List<Account> findByUserId(Long userId);

	Page<Account> findByUserId(Long userId, Pageable pageable);

	Optional<Account> findByUserIdAndIsPrimaryTrue(Long userId);

	List<Account> findByUserIdAndStatus(Long userId, AccountStatus status);

	List<Account> findByUserIdAndAccountType(Long userId, AccountType accountType);

	// Existence checks
	boolean existsByAccountNumber(String accountNumber);

	boolean existsByUserIdAndIsPrimaryTrue(Long userId);

	boolean existsByUserIdAndAccountTypeAndStatus(Long userId, AccountType accountType, AccountStatus status);

	// Count operations
	long countByUserId(Long userId);

	long countByStatus(AccountStatus status);

	long countByAccountType(AccountType accountType);

	@Query("SELECT COUNT(a) FROM Account a WHERE a.userId = :userId AND a.status = :status")
	long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") AccountStatus status);

	// Balance queries
	@Query("SELECT SUM(a.balance) FROM Account a WHERE a.status = :status")
	BigDecimal sumBalanceByStatus(@Param("status") AccountStatus status);

	@Query("SELECT SUM(a.balance) FROM Account a WHERE a.userId = :userId")
	BigDecimal sumBalanceByUserId(@Param("userId") Long userId);

	@Query("SELECT a FROM Account a WHERE a.balance < a.minimumBalance AND a.status = :status")
	List<Account> findAccountsBelowMinimumBalance(@Param("status") AccountStatus status);

	// Update operations
	@Modifying
	@Query("UPDATE Account a SET a.balance = a.balance + :amount, a.availableBalance = a.availableBalance + :amount WHERE a.id = :accountId")
	int creditAccount(@Param("accountId") Long accountId, @Param("amount") BigDecimal amount);

	@Modifying
	@Query("UPDATE Account a SET a.balance = a.balance - :amount, a.availableBalance = a.availableBalance - :amount WHERE a.id = :accountId AND a.availableBalance >= :amount")
	int debitAccount(@Param("accountId") Long accountId, @Param("amount") BigDecimal amount);

	@Modifying
	@Query("UPDATE Account a SET a.status = :status, a.updatedBy = :updatedBy WHERE a.id = :accountId")
	int updateStatus(@Param("accountId") Long accountId, @Param("status") AccountStatus status,
			@Param("updatedBy") String updatedBy);

	@Modifying
	@Query("UPDATE Account a SET a.status = :newStatus, a.frozenReason = :reason, a.updatedBy = :updatedBy WHERE a.id = :accountId")
	int freezeAccount(@Param("accountId") Long accountId, @Param("newStatus") AccountStatus newStatus,
			@Param("reason") String reason, @Param("updatedBy") String updatedBy);

	@Modifying
	@Query("UPDATE Account a SET a.status = :status, a.closedAt = :closedAt, a.closureReason = :reason WHERE a.id = :accountId")
	int closeAccount(@Param("accountId") Long accountId, @Param("status") AccountStatus status,
			@Param("closedAt") LocalDateTime closedAt, @Param("reason") String reason);

	// Dormant accounts
	@Query("SELECT a FROM Account a WHERE a.status = 'ACTIVE' AND a.updatedAt < :dormantDate")
	List<Account> findDormantAccounts(@Param("dormantDate") LocalDateTime dormantDate);

	// Statistics
	@Query("SELECT a.accountType as type, COUNT(a) as count FROM Account a WHERE a.status = :status GROUP BY a.accountType")
	List<Object[]> countByAccountTypeAndStatus(@Param("status") AccountStatus status);

	@Query("SELECT a.status as status, COUNT(a) as count FROM Account a GROUP BY a.status")
	List<Object[]> countByStatus();

	// Search
	@Query("SELECT a FROM Account a WHERE " + "(:userId IS NULL OR a.userId = :userId) AND "
			+ "(:accountNumber IS NULL OR a.accountNumber LIKE %:accountNumber%) AND "
			+ "(:status IS NULL OR a.status = :status) AND " + "(:accountType IS NULL OR a.accountType = :accountType)")
	Page<Account> searchAccounts(@Param("userId") Long userId, @Param("accountNumber") String accountNumber,
			@Param("status") AccountStatus status, @Param("accountType") AccountType accountType, Pageable pageable);
}
