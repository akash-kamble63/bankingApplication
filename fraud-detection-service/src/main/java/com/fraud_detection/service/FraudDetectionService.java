package com.fraud_detection.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.fraud_detection.dto.FraudCheckDetailDTO;
import com.fraud_detection.dto.FraudCheckRequestDTO;
import com.fraud_detection.dto.FraudCheckResponseDTO;
import com.fraud_detection.dto.FraudReviewRequestDTO;
import com.fraud_detection.dto.FraudStatisticsDTO;
import com.fraud_detection.enums.FraudStatus;

public interface FraudDetectionService {
	FraudCheckResponseDTO checkFraud(FraudCheckRequestDTO request);

	FraudCheckDetailDTO reviewFraudCheck(Long fraudCheckId, FraudReviewRequestDTO reviewRequest, String reviewerUserId);

	FraudCheckDetailDTO getFraudCheckByTransaction(String transactionId);

	Page<FraudCheckDetailDTO> getFraudChecksByStatus(FraudStatus status, Pageable pageable);

	Page<FraudCheckDetailDTO> getPendingReviews(Pageable pageable);

	Page<FraudCheckDetailDTO> getFraudChecksByAccount(String accountId, Pageable pageable);

	FraudStatisticsDTO getStatistics(LocalDateTime since);

	FraudCheckResponseDTO checkFraudFallback(FraudCheckRequestDTO request, Exception ex);

}
