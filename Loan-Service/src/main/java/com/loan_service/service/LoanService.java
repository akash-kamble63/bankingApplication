package com.loan_service.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.loan_service.dto.EmiScheduleResponse;
import com.loan_service.dto.LoanApplicationRequest;
import com.loan_service.dto.LoanApplicationResponse;
import com.loan_service.dto.LoanApprovalRequest;
import com.loan_service.dto.LoanRejectionRequest;
import com.loan_service.dto.LoanResponse;
import com.loan_service.dto.LoanSummaryResponse;
import com.loan_service.dto.PrepaymentRequest;
import com.loan_service.dto.PrepaymentResponse;

public interface LoanService {
	LoanApplicationResponse applyForLoan(LoanApplicationRequest request, Long userId);
    LoanResponse approveLoan(LoanApprovalRequest request, Long approvedBy);
    LoanResponse rejectLoan(LoanRejectionRequest request, Long reviewedBy);
    LoanResponse disburseLoan(String loanNumber, Long userId);
    LoanResponse getLoan(String loanNumber, Long userId);
    Page<LoanResponse> getUserLoans(Long userId, Pageable pageable);
    List<EmiScheduleResponse> getEmiSchedule(String loanNumber, Long userId);
    void processEmiPayment(String loanNumber, LocalDate emiDate);
    PrepaymentResponse prepayLoan(String loanNumber, PrepaymentRequest request, Long userId);
    LoanResponse foreCloseLoan(String loanNumber, Long userId);
    LoanSummaryResponse getLoanSummary(Long userId);
}
