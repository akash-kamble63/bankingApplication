package com.account_service.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.account_service.model.AccountBalanceSnapshot;

public interface AccountBalanceSnapshotRepository extends JpaRepository<AccountBalanceSnapshot, Long> {
    
    // Find operations
    List<AccountBalanceSnapshot> findByAccountId(Long accountId);
    
    Page<AccountBalanceSnapshot> findByAccountId(Long accountId, Pageable pageable);
    
    List<AccountBalanceSnapshot> findByAccountIdAndSnapshotType(Long accountId, String snapshotType);
    
    Optional<AccountBalanceSnapshot> findByAccountIdAndSnapshotDateAndSnapshotType(
        Long accountId, LocalDateTime snapshotDate, String snapshotType);
    
    // Date range queries
    @Query("SELECT s FROM AccountBalanceSnapshot s WHERE s.accountId = :accountId " +
           "AND s.snapshotDate BETWEEN :startDate AND :endDate " +
           "ORDER BY s.snapshotDate DESC")
    List<AccountBalanceSnapshot> findByAccountIdAndDateRange(
        @Param("accountId") Long accountId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    // Latest snapshot
    @Query("SELECT s FROM AccountBalanceSnapshot s WHERE s.accountId = :accountId " +
           "ORDER BY s.snapshotDate DESC LIMIT 1")
    Optional<AccountBalanceSnapshot> findLatestSnapshot(@Param("accountId") Long accountId);
    
    // Existence check
    boolean existsByAccountIdAndSnapshotDateAndSnapshotType(
        Long accountId, LocalDateTime snapshotDate, String snapshotType);
}
