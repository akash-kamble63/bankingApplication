package com.loan_service.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.loan_service.dto.EmiScheduleResponse;
import com.loan_service.dto.LoanApplicationRequest;
import com.loan_service.dto.LoanApplicationResponse;
import com.loan_service.dto.LoanApprovalRequest;
import com.loan_service.dto.LoanRejectionRequest;
import com.loan_service.dto.LoanResponse;
import com.loan_service.dto.LoanSummaryResponse;
import com.loan_service.dto.PrepaymentRequest;
import com.loan_service.dto.PrepaymentResponse;
import com.loan_service.entity.LoanApplication;
import com.loan_service.exception.ResourceNotFoundException;
import com.loan_service.exception.UnauthorizedAccessException;
import com.loan_service.service.LoanApplicationService;
import com.loan_service.service.LoanService;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
@Tag(name = "Loan Management", description = "Loan management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class LoanController {
	private final LoanService loanService;
	private final LoanApplicationService loanApplicationService;
	

    @PostMapping("/applications")
    @PreAuthorize("hasRole('USER')")
    @Timed(value = "loan.application", description = "Time to process loan application")
    @Operation(summary = "Apply for loan", description = "Submit a new loan application")
    public ResponseEntity<LoanApplicationResponse> applyForLoan(
            @Valid @RequestBody LoanApplicationRequest request,
            Authentication authentication) {
        
        Long userId = extractUserId(authentication);
        LoanApplicationResponse response = loanService.applyForLoan(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/applications/approve")
    @PreAuthorize("hasRole('LOAN_OFFICER')")
    @Operation(summary = "Approve loan", description = "Approve a loan application")
    public ResponseEntity<LoanResponse> approveLoan(
            @Valid @RequestBody LoanApprovalRequest request,
            Authentication authentication) {
        
        Long approvedBy = extractUserId(authentication);
        LoanResponse response = loanService.approveLoan(request, approvedBy);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{loanNumber}/disburse")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Disburse loan", description = "Disburse approved loan")
    public ResponseEntity<LoanResponse> disburseLoan(
            @PathVariable String loanNumber,
            Authentication authentication) {
        
        Long userId = extractUserId(authentication);
        LoanResponse response = loanService.disburseLoan(loanNumber, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get user loans", description = "Get all loans of user")
    public ResponseEntity<Page<LoanResponse>> getUserLoans(
            Authentication authentication,
            Pageable pageable) {
        
        Long userId = extractUserId(authentication);
        Page<LoanResponse> loans = loanService.getUserLoans(userId, pageable);
        return ResponseEntity.ok(loans);
    }

    @GetMapping("/{loanNumber}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get loan details")
    public ResponseEntity<LoanResponse> getLoan(
            @PathVariable String loanNumber,
            Authentication authentication) {
        
        Long userId = extractUserId(authentication);
        LoanResponse loan = loanService.getLoan(loanNumber, userId);
        return ResponseEntity.ok(loan);
    }

    @GetMapping("/{loanNumber}/emi-schedule")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get EMI schedule", description = "Get complete EMI schedule")
    public ResponseEntity<List<EmiScheduleResponse>> getEmiSchedule(
            @PathVariable String loanNumber,
            Authentication authentication) {
        
        Long userId = extractUserId(authentication);
        List<EmiScheduleResponse> schedule = loanService.getEmiSchedule(loanNumber, userId);
        return ResponseEntity.ok(schedule);
    }

    @PostMapping("/{loanNumber}/prepay")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Prepay loan", description = "Make prepayment on loan")
    public ResponseEntity<PrepaymentResponse> prepayLoan(
            @PathVariable String loanNumber,
            @Valid @RequestBody PrepaymentRequest request,
            Authentication authentication) {
        
        Long userId = extractUserId(authentication);
        PrepaymentResponse response = loanService.prepayLoan(loanNumber, request, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{loanNumber}/foreclose")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Foreclose loan", description = "Close loan with full payment")
    public ResponseEntity<LoanResponse> foreCloseLoan(
            @PathVariable String loanNumber,
            Authentication authentication) {
        
        Long userId = extractUserId(authentication);
        LoanResponse response = loanService.foreCloseLoan(loanNumber, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Loan Service is healthy");
    }

    private Long extractUserId(Authentication authentication) {
        return Long.parseLong(authentication.getName());
    }
    
    @GetMapping("/summary")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get loan summary", description = "Get user's complete loan summary")
    public ResponseEntity<LoanSummaryResponse> getLoanSummary(Authentication authentication) {
        Long userId = extractUserId(authentication);
        LoanSummaryResponse summary = loanService.getLoanSummary(userId);
        return ResponseEntity.ok(summary);
    }

    @PostMapping("/applications/{applicationId}/reject")
    @PreAuthorize("hasRole('LOAN_OFFICER')")
    @Operation(summary = "Reject loan application")
    public ResponseEntity<Void> rejectLoan(
            @PathVariable Long applicationId,
            @Valid @RequestBody LoanRejectionRequest request,
            Authentication authentication) {
        
        Long reviewedBy = extractUserId(authentication);
        request.setApplicationId(applicationId);
        loanService.rejectLoan(request, reviewedBy);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/applications/{applicationNumber}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get application details")
    public ResponseEntity<LoanApplicationResponse> getApplication(
            @PathVariable String applicationNumber,
            Authentication authentication) {
        
    	Long userId = extractUserId(authentication);

        LoanApplicationResponse response =
                loanApplicationService.getApplication(applicationNumber, userId);

        return ResponseEntity.ok(response);
    }
    
}
