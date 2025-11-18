package com.loan_service.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.loan_service.entity.Loan;
import com.loan_service.enums.LoanStatus;

import jakarta.persistence.LockModeType;

public interface LoanRepository extends JpaRepository<Loan, Long>{
	@Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Loan l WHERE l.loanNumber = :loanNumber")
    Optional<Loan> findByLoanNumberForUpdate(@Param("loanNumber") String loanNumber);
    
    Optional<Loan> findByLoanNumber(String loanNumber);
    
    @Query("SELECT l FROM Loan l WHERE l.userId = :userId ORDER BY l.createdAt DESC")
    Page<Loan> findByUserId(@Param("userId") Long userId, Pageable pageable);
    
    @Query("SELECT l FROM Loan l WHERE l.userId = :userId AND l.status = :status")
    Page<Loan> findByUserIdAndStatus(
        @Param("userId") Long userId,
        @Param("status") LoanStatus status,
        Pageable pageable
    );
    
    @Query("SELECT l FROM Loan l WHERE l.status IN ('ACTIVE', 'OVERDUE') " +
           "AND l.nextEmiDate = :date")
    List<Loan> findLoansWithEmiDueOn(@Param("date") LocalDate date);
    
    @Query("SELECT l FROM Loan l WHERE l.status = 'ACTIVE' " +
           "AND l.nextEmiDate < :date")
    List<Loan> findOverdueLoans(@Param("date") LocalDate date);
    
    @Query("SELECT SUM(l.outstandingPrincipal) FROM Loan l " +
           "WHERE l.userId = :userId AND l.status IN ('ACTIVE', 'OVERDUE')")
    BigDecimal getTotalOutstandingByUser(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(l) FROM Loan l WHERE l.userId = :userId " +
           "AND l.status IN ('ACTIVE', 'OVERDUE')")
    Long countActiveLoans(@Param("userId") Long userId);
    
    @Query("SELECT l FROM Loan l WHERE l.status = 'OVERDUE' " +
           "AND l.missedEmis >= :threshold")
    List<Loan> findDefaultedLoans(@Param("threshold") Integer threshold);
}
