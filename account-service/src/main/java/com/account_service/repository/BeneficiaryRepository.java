package com.account_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.account_service.enums.BeneficiaryStatus;
import com.account_service.model.Beneficiary;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {
    
    // Find operations
    List<Beneficiary> findByUserId(Long userId);
    
    Page<Beneficiary> findByUserId(Long userId, Pageable pageable);
    
    List<Beneficiary> findByUserIdAndStatus(Long userId, BeneficiaryStatus status);
    
    Optional<Beneficiary> findByUserIdAndBeneficiaryAccountNumber(Long userId, String accountNumber);
    
    boolean existsByUserIdAndBeneficiaryAccountNumber(Long userId, String accountNumber);
    
    // Count operations
    long countByUserId(Long userId);
    
    long countByUserIdAndStatus(Long userId, BeneficiaryStatus status);
    
    // Update operations
    @Modifying
    @Query("UPDATE Beneficiary b SET b.status = :status WHERE b.id = :beneficiaryId")
    int updateStatus(@Param("beneficiaryId") Long beneficiaryId, @Param("status") BeneficiaryStatus status);
    
    @Modifying
    @Query("UPDATE Beneficiary b SET b.isVerified = true, b.verifiedAt = CURRENT_TIMESTAMP, b.status = 'VERIFIED' WHERE b.id = :beneficiaryId")
    int verifyBeneficiary(@Param("beneficiaryId") Long beneficiaryId);
    
    // Search
    @Query("SELECT b FROM Beneficiary b WHERE b.userId = :userId AND " +
           "(LOWER(b.beneficiaryName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "b.beneficiaryAccountNumber LIKE CONCAT('%', :searchTerm, '%') OR " +
           "LOWER(b.nickname) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<Beneficiary> searchBeneficiaries(@Param("userId") Long userId, @Param("searchTerm") String searchTerm);
}