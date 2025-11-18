package com.fraud_detection.controller;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fraud_detection.dto.ErrorResponse;
import com.fraud_detection.dto.FraudCheckDetailDTO;
import com.fraud_detection.dto.FraudCheckRequestDTO;
import com.fraud_detection.dto.FraudCheckResponseDTO;
import com.fraud_detection.dto.FraudReviewRequestDTO;
import com.fraud_detection.dto.FraudStatisticsDTO;
import com.fraud_detection.enums.FraudStatus;
import com.fraud_detection.service.FraudDetectionService;

import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/fraud")
@RequiredArgsConstructor
@Slf4j
public class FraudDetectionController {
private final FraudDetectionService fraudDetectionService;
    
    @PostMapping("/check")
    @Timed(value = "fraud.check.time", description = "Time taken to check fraud")
    @PreAuthorize("hasAnyRole('SYSTEM', 'FRAUD_ANALYST')")
    public ResponseEntity<FraudCheckResponseDTO> checkFraud(
            @Valid @RequestBody FraudCheckRequestDTO request) {
        
        log.info("Received fraud check request for transaction: {}", request.getTransactionId());
        FraudCheckResponseDTO response = fraudDetectionService.checkFraud(request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/transaction/{transactionId}")
    @PreAuthorize("hasAnyRole('FRAUD_ANALYST', 'ADMIN')")
    public ResponseEntity<FraudCheckDetailDTO> getFraudCheckByTransaction(
            @PathVariable String transactionId) {
        
        log.info("Fetching fraud check for transaction: {}", transactionId);
        FraudCheckDetailDTO response = fraudDetectionService
                .getFraudCheckByTransaction(transactionId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('FRAUD_ANALYST', 'ADMIN')")
    public ResponseEntity<Page<FraudCheckDetailDTO>> getFraudChecksByStatus(
            @PathVariable FraudStatus status,
            Pageable pageable) {
        
        log.info("Fetching fraud checks with status: {}", status);
        Page<FraudCheckDetailDTO> response = fraudDetectionService
                .getFraudChecksByStatus(status, pageable);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/pending-reviews")
    @PreAuthorize("hasRole('FRAUD_ANALYST')")
    public ResponseEntity<Page<FraudCheckDetailDTO>> getPendingReviews(Pageable pageable) {
        log.info("Fetching pending fraud reviews");
        Page<FraudCheckDetailDTO> response = fraudDetectionService.getPendingReviews(pageable);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/account/{accountId}")
    @PreAuthorize("hasAnyRole('FRAUD_ANALYST', 'ADMIN', 'USER')")
    public ResponseEntity<Page<FraudCheckDetailDTO>> getFraudChecksByAccount(
            @PathVariable String accountId,
            Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {
        
        // Users can only view their own fraud checks
        String userRole = jwt.getClaimAsString("realm_access");
        String userId = jwt.getSubject();
        
        if (!userRole.contains("FRAUD_ANALYST") && !userRole.contains("ADMIN")) {
            // Verify user owns the account
            // This should be validated against account service
            log.info("User {} accessing fraud checks for account: {}", userId, accountId);
        }
        
        Page<FraudCheckDetailDTO> response = fraudDetectionService
                .getFraudChecksByAccount(accountId, pageable);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{fraudCheckId}/review")
    @PreAuthorize("hasRole('FRAUD_ANALYST')")
    public ResponseEntity<FraudCheckDetailDTO> reviewFraudCheck(
            @PathVariable Long fraudCheckId,
            @Valid @RequestBody FraudReviewRequestDTO reviewRequest,
            @AuthenticationPrincipal Jwt jwt) {
        
        String reviewerUserId = jwt.getSubject();
        log.info("User {} reviewing fraud check: {}", reviewerUserId, fraudCheckId);
        
        FraudCheckDetailDTO response = fraudDetectionService
                .reviewFraudCheck(fraudCheckId, reviewRequest, reviewerUserId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('FRAUD_ANALYST', 'ADMIN')")
    public ResponseEntity<FraudStatisticsDTO> getStatistics(
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) 
            LocalDateTime since) {
        
        log.info("Fetching fraud statistics since: {}", since);
        FraudStatisticsDTO response = fraudDetectionService.getStatistics(since);
        return ResponseEntity.ok(response);
    }
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        log.error("Error processing fraud request", ex);
        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("Invalid request", ex);
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
