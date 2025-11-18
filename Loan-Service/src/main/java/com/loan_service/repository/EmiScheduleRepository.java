package com.loan_service.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.loan_service.entity.EmiSchedule;
import com.loan_service.enums.EmiStatus;

public interface EmiScheduleRepository extends JpaRepository<EmiSchedule, Long>{
	@Query("SELECT es FROM EmiSchedule es WHERE es.loanId = :loanId " +
	           "ORDER BY es.emiNumber ASC")
	    List<EmiSchedule> findByLoanId(@Param("loanId") Long loanId);
	    
	    @Query("SELECT es FROM EmiSchedule es WHERE es.loanId = :loanId " +
	           "AND es.status = :status ORDER BY es.emiNumber ASC")
	    List<EmiSchedule> findByLoanIdAndStatus(
	        @Param("loanId") Long loanId,
	        @Param("status") EmiStatus status
	    );
	    
	    @Query("SELECT es FROM EmiSchedule es WHERE es.loanId = :loanId " +
	           "AND es.dueDate = :dueDate")
	    Optional<EmiSchedule> findByLoanIdAndDueDate(
	        @Param("loanId") Long loanId,
	        @Param("dueDate") LocalDate dueDate
	    );
	    
	    @Query("SELECT es FROM EmiSchedule es WHERE es.dueDate = :date " +
	           "AND es.status = 'SCHEDULED'")
	    List<EmiSchedule> findScheduledEmisForDate(@Param("date") LocalDate date);
	    
	    @Query("SELECT es FROM EmiSchedule es WHERE es.dueDate < :date " +
	           "AND es.status IN ('SCHEDULED', 'PENDING')")
	    List<EmiSchedule> findOverdueEmis(@Param("date") LocalDate date);
	    
}
