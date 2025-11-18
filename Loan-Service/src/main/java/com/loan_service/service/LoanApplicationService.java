package com.loan_service.service;

import org.springframework.stereotype.Service;

import com.loan_service.dto.LoanApplicationResponse;
import com.loan_service.entity.LoanApplication;
import com.loan_service.exception.ResourceNotFoundException;
import com.loan_service.exception.UnauthorizedAccessException;
import com.loan_service.repository.LoanApplicationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoanApplicationService {
	private final LoanApplicationRepository applicationRepository;

    public LoanApplicationResponse getApplication(String applicationNumber, Long userId) {

    	LoanApplication application = applicationRepository
                .findByApplicationNumber(applicationNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        if (!application.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("Unauthorized access");
        }

        return mapApplicationToResponse(application);
    }

    private LoanApplicationResponse mapApplicationToResponse(LoanApplication application) {
    	return LoanApplicationResponse.builder()
                .id(application.getId())
                .applicationNumber(application.getApplicationNumber())
                .loanType(application.getLoanType())
                .status(application.getStatus())
                .requestedAmount(application.getRequestedAmount())
                .requestedTenureMonths(application.getRequestedTenureMonths())
                .creditScore(application.getCreditScore())
                .fraudScore(application.getFraudScore())
                .rejectionReason(application.getRejectionReason())
                .createdAt(application.getCreatedAt())
                .build();
    }
}
