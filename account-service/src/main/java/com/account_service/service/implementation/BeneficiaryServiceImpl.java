package com.account_service.service.implementation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.account_service.dto.BeneficiaryResponse;
import com.account_service.dto.CreateBeneficiaryRequest;
import com.account_service.enums.BeneficiaryStatus;
import com.account_service.exception.ResourceConflictException;
import com.account_service.exception.ResourceNotFoundException;
import com.account_service.model.Beneficiary;
import com.account_service.repository.BeneficiaryRepository;
import com.account_service.service.BeneficiaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BeneficiaryServiceImpl implements BeneficiaryService {
    
    private final BeneficiaryRepository beneficiaryRepository;
    
    @Override
    @Transactional
    public BeneficiaryResponse addBeneficiary(CreateBeneficiaryRequest request) {
        log.info("Adding beneficiary for user: {}", request.getUserId());
        
        // Check for duplicate
        if (beneficiaryRepository.existsByUserIdAndBeneficiaryAccountNumber(
                request.getUserId(), request.getBeneficiaryAccountNumber())) {
            throw new ResourceConflictException("Beneficiary already exists");
        }
        
        Beneficiary beneficiary = Beneficiary.builder()
                .userId(request.getUserId())
                .accountId(request.getAccountId())
                .beneficiaryName(request.getBeneficiaryName())
                .beneficiaryAccountNumber(request.getBeneficiaryAccountNumber())
                .beneficiaryIfsc(request.getBeneficiaryIfsc())
                .beneficiaryBank(request.getBeneficiaryBank())
                .nickname(request.getNickname())
                .status(BeneficiaryStatus.PENDING_VERIFICATION)
                .isVerified(false)
                .build();
        
        beneficiary = beneficiaryRepository.save(beneficiary);
        log.info("Beneficiary added: {}", beneficiary.getId());
        
        return mapToResponse(beneficiary);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<BeneficiaryResponse> getUserBeneficiaries(Long userId) {
        return beneficiaryRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public BeneficiaryResponse verifyBeneficiary(Long beneficiaryId) {
        Beneficiary beneficiary = beneficiaryRepository.findById(beneficiaryId)
                .orElseThrow(() -> new ResourceNotFoundException("Beneficiary not found"));
        
        beneficiary.setIsVerified(true);
        beneficiary.setVerifiedAt(LocalDateTime.now());
        beneficiary.setStatus(BeneficiaryStatus.VERIFIED);
        
        beneficiary = beneficiaryRepository.save(beneficiary);
        log.info("Beneficiary verified: {}", beneficiaryId);
        
        return mapToResponse(beneficiary);
    }
    
    @Override
    @Transactional
    public void deleteBeneficiary(Long beneficiaryId) {
        if (!beneficiaryRepository.existsById(beneficiaryId)) {
            throw new ResourceNotFoundException("Beneficiary not found");
        }
        beneficiaryRepository.deleteById(beneficiaryId);
        log.info("Beneficiary deleted: {}", beneficiaryId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<BeneficiaryResponse> searchBeneficiaries(Long userId, String searchTerm, Pageable pageable) {
        // Implement search logic
        return Page.empty();
    }
    
    private BeneficiaryResponse mapToResponse(Beneficiary beneficiary) {
        return BeneficiaryResponse.builder()
                .id(beneficiary.getId())
                .userId(beneficiary.getUserId())
                .accountId(beneficiary.getAccountId())
                .beneficiaryName(beneficiary.getBeneficiaryName())
                .beneficiaryAccountNumber(beneficiary.getBeneficiaryAccountNumber())
                .beneficiaryIfsc(beneficiary.getBeneficiaryIfsc())
                .beneficiaryBank(beneficiary.getBeneficiaryBank())
                .nickname(beneficiary.getNickname())
                .status(beneficiary.getStatus())
                .isVerified(beneficiary.getIsVerified())
                .verifiedAt(beneficiary.getVerifiedAt())
                .createdAt(beneficiary.getCreatedAt())
                .build();
    }
}