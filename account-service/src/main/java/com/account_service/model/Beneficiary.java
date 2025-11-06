package com.account_service.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.account_service.enums.BeneficiaryStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "beneficiaries", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_account_id", columnList = "account_id")
})
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Beneficiary {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId; // Owner of this beneficiary list
    
    @Column(name = "account_id")
    private Long accountId; // Owner's account (optional)
    
    @Column(name = "beneficiary_name", nullable = false, length = 100)
    private String beneficiaryName;
    
    @Column(name = "beneficiary_account_number", nullable = false, length = 20)
    private String beneficiaryAccountNumber;
    
    @Column(name = "beneficiary_ifsc", length = 11)
    private String beneficiaryIfsc;
    
    @Column(name = "beneficiary_bank", length = 100)
    private String beneficiaryBank;
    
    @Column(name = "nickname", length = 50)
    private String nickname;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BeneficiaryStatus status; // PENDING_VERIFICATION, VERIFIED, BLOCKED
    
    @Column(name = "is_verified")
    private Boolean isVerified = false;
    
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
