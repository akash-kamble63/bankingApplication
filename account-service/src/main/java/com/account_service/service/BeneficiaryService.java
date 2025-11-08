package com.account_service.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.account_service.dto.BeneficiaryResponse;
import com.account_service.dto.CreateBeneficiaryRequest;

public interface BeneficiaryService {
	public BeneficiaryResponse addBeneficiary(CreateBeneficiaryRequest request);
	public List<BeneficiaryResponse> getUserBeneficiaries(Long userId);
	public BeneficiaryResponse verifyBeneficiary(Long beneficiaryId);
	public void deleteBeneficiary(Long beneficiaryId);
	public Page<BeneficiaryResponse> searchBeneficiaries(Long userId, String searchTerm, Pageable pageable) ;

}
