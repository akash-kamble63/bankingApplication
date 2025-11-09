package com.transaction_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.jpa.domain.Specification;
import com.transaction_service.entity.Transaction;
import com.transaction_service.enums.*;

import jakarta.persistence.LockModeType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaSpecificationExecutor<Transaction>, JpaRepository<Transaction, Long> {
    Optional<Transaction> findByTransactionReference(String transactionReference);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Transaction t WHERE t.transactionReference = :ref")
    Optional<Transaction> findByTransactionReferenceForUpdate(String ref);
    
    Page<Transaction> findByUserId(Long userId, Pageable pageable);
    
    Page<Transaction> findByAccountNumber(String accountNumber, Pageable pageable);
    
    List<Transaction> findByAccountNumberAndStatus(String accountNumber, TransactionStatus status);
    
    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId " +
           "AND t.createdAt BETWEEN :start AND :end")
    Page<Transaction> findByUserIdAndDateRange(Long userId, LocalDateTime start, 
                                               LocalDateTime end, Pageable pageable);
    
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.accountNumber = :account " +
           "AND t.transactionType = :type AND t.status = 'SUCCESS' " +
           "AND t.createdAt BETWEEN :start AND :end")
    BigDecimal sumAmountByTypeAndDateRange(String account, TransactionType type, 
                                           LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.accountNumber = :account " +
           "AND t.status = :status AND t.createdAt BETWEEN :start AND :end")
    long countByAccountAndStatusAndDateRange(String account, TransactionStatus status,
                                            LocalDateTime start, LocalDateTime end);
    
    boolean existsByTransactionReference(String transactionReference);
    
    @Query("SELECT t FROM Transaction t WHERE t.status IN ('INITIATED', 'PENDING', 'PROCESSING') " +
           "AND t.createdAt < :timeout")
    List<Transaction> findTimeoutTransactions(LocalDateTime timeout);
}
