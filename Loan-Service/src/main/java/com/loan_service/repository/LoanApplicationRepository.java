package com.loan_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.loan_service.entity.LoanApplication;
import com.loan_service.enums.ApplicationStatus;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {
	Optional<LoanApplication> findByApplicationNumber(String applicationNumber);

	@Query("SELECT la FROM LoanApplication la WHERE la.userId = :userId " + "ORDER BY la.createdAt DESC")
	Page<LoanApplication> findByUserId(@Param("userId") Long userId, Pageable pageable);

	@Query("SELECT la FROM LoanApplication la WHERE la.status = :status " + "ORDER BY la.createdAt ASC")
	List<LoanApplication> findByStatus(@Param("status") ApplicationStatus status);

	@Query("SELECT la FROM LoanApplication la WHERE la.assignedTo = :assignedTo " + "AND la.status = 'UNDER_REVIEW'")
	Page<LoanApplication> findAssignedApplications(@Param("assignedTo") Long assignedTo, Pageable pageable);
}
