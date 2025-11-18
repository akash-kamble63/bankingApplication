package com.loan_service.service.implementation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.account_service.service.EventSourcingService;
import com.account_service.service.OutboxService;
import com.loan_service.client.AccountServiceClient;
import com.loan_service.client.FraudCheckResult;
import com.loan_service.client.FraudServiceClient;
import com.loan_service.dto.CreditScoreResponse;
import com.loan_service.dto.EmiCalculationResult;
import com.loan_service.dto.EmiScheduleResponse;
import com.loan_service.dto.LoanApplicationRequest;
import com.loan_service.dto.LoanApplicationResponse;
import com.loan_service.dto.LoanApprovalRequest;
import com.loan_service.dto.LoanRejectionRequest;
import com.loan_service.dto.LoanResponse;
import com.loan_service.dto.LoanSummaryResponse;
import com.loan_service.dto.PrepaymentRequest;
import com.loan_service.dto.PrepaymentResponse;
import com.loan_service.entity.EmiSchedule;
import com.loan_service.entity.Loan;
import com.loan_service.entity.LoanApplication;
import com.loan_service.enums.ApplicationStatus;
import com.loan_service.enums.EmiStatus;
import com.loan_service.enums.InterestType;
import com.loan_service.enums.LoanStatus;
import com.loan_service.exception.InvalidLoanOperationException;
import com.loan_service.exception.LoanApplicationException;
import com.loan_service.exception.ResourceNotFoundException;
import com.loan_service.exception.UnauthorizedAccessException;
import com.loan_service.repository.EmiScheduleRepository;
import com.loan_service.repository.LoanApplicationRepository;
import com.loan_service.repository.LoanRepository;
import com.loan_service.service.CreditScoringService;
import com.loan_service.service.EmiCalculatorService;
import com.loan_service.service.LoanService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
@RequiredArgsConstructor
public class LoanServiceImpl implements LoanService{
    private final LoanRepository loanRepository;
    private final LoanApplicationRepository applicationRepository;
    private final EmiScheduleRepository emiScheduleRepository;
    private final EmiCalculatorService emiCalculator;
    private final CreditScoringService creditScoringService;
    private final AccountServiceClient accountServiceClient;
    private final FraudServiceClient fraudServiceClient;
    private final OutboxService outboxService; 
    private final EventSourcingService eventSourcingService;

    @Override
    @Transactional
    public LoanApplicationResponse applyForLoan(LoanApplicationRequest request, Long userId) {
        log.info("Processing loan application: userId={}, type={}, amount={}", 
            userId, request.getLoanType(), request.getRequestedAmount());
        
        // Validate user has no active defaults
        List<Loan> overdueLoans = loanRepository.findOverdueLoans(LocalDate.now());
        boolean hasDefault = overdueLoans.stream()
            .anyMatch(l -> l.getUserId().equals(userId) && l.getMissedEmis() >= 3);
        
        if (hasDefault) {
            throw new LoanApplicationException("Cannot apply - existing loan default");
        }
        
        // Validate account
        if (!accountServiceClient.validateAccountOwnership(request.getAccountId(), userId)) {
            throw new UnauthorizedAccessException("Account does not belong to user");
        }
        
        // Generate application number
        String applicationNumber = generateApplicationNumber();
        
        // Create application
        LoanApplication application = LoanApplication.builder()
            .applicationNumber(applicationNumber)
            .userId(userId)
            .accountId(request.getAccountId())
            .loanType(request.getLoanType())
            .status(ApplicationStatus.SUBMITTED)
            .requestedAmount(request.getRequestedAmount())
            .requestedTenureMonths(request.getRequestedTenureMonths())
            .loanPurpose(request.getLoanPurpose())
            .annualIncome(request.getAnnualIncome())
            .employmentType(request.getEmploymentType())
            .companyName(request.getCompanyName())
            .monthlyObligations(request.getMonthlyObligations())
            .collateralOffered(request.getCollateralOffered())
            .collateralDetails(request.getCollateralDetails())
            .build();
        
        application = applicationRepository.save(application);
        
        // Trigger async processing
        processApplicationAsync(application.getId());
        
        // Publish event using Loan Service's outbox
        outboxService.saveEvent("LOAN_APPLICATION", applicationNumber,
            "LoanApplicationSubmitted", "banking.loan.applications", application);
        
        return mapApplicationToResponse(application);
    }

    @Transactional
    public void processApplicationAsync(Long applicationId) {
        LoanApplication application = applicationRepository.findById(applicationId)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        
        try {
            // Fraud check
            application.setStatus(ApplicationStatus.UNDER_REVIEW);
            applicationRepository.save(application);
            
            FraudCheckResult fraudCheck = fraudServiceClient.checkLoanApplication(
                application.getUserId(),
                application.getRequestedAmount(),
                application.getAnnualIncome()
            );
            
            application.setFraudScore(fraudCheck.getFraudScore());
            
            if (fraudCheck.isBlocked()) {
                application.setStatus(ApplicationStatus.REJECTED);
                application.setRejectionReason("Failed fraud check");
                applicationRepository.save(application);
                return;
            }
            
            // Credit check
            application.setStatus(ApplicationStatus.CREDIT_CHECK_PENDING);
            applicationRepository.save(application);
            
            CreditScoreResponse creditScore = creditScoringService
                .fetchCreditScore(application.getUserId(), "PAN123456"); // Get from user service
            
            application.setCreditScore(creditScore.getScore());
            application.setStatus(ApplicationStatus.CREDIT_CHECK_COMPLETED);
            applicationRepository.save(application);
            
            // Check minimum credit score
            if (creditScore.getScore() < 650) {
                application.setStatus(ApplicationStatus.REJECTED);
                application.setRejectionReason("Credit score below minimum requirement");
                applicationRepository.save(application);
                return;
            }
            
            // Eligibility check
            BigDecimal maxEligibleAmount = calculateMaxEligibleAmount(
                application.getAnnualIncome(),
                application.getMonthlyObligations()
            );
            
            if (application.getRequestedAmount().compareTo(maxEligibleAmount) > 0) {
                application.setStatus(ApplicationStatus.REJECTED);
                application.setRejectionReason("Requested amount exceeds eligibility");
                applicationRepository.save(application);
                return;
            }
            
            // Auto-approve if conditions met
            if (creditScore.getScore() >= 750 && 
                application.getRequestedAmount().compareTo(new BigDecimal("500000")) <= 0) {
                
                BigDecimal interestRate = creditScoringService.calculateInterestRate(
                    creditScore.getScore(),
                    application.getRequestedAmount(),
                    application.getRequestedTenureMonths()
                );
                
                autoApproveLoan(application, interestRate);
            } else {
                // Manual review required
                application.setStatus(ApplicationStatus.DOCUMENTS_PENDING);
                applicationRepository.save(application);
            }
            
        } catch (Exception e) {
            log.error("Application processing failed: {}", e.getMessage(), e);
            application.setStatus(ApplicationStatus.UNDER_REVIEW);
            applicationRepository.save(application);
        }
    }

    @Override
    @Transactional
    public LoanResponse approveLoan(LoanApprovalRequest request, Long approvedBy) {
        log.info("Approving loan application: {}", request.getApplicationId());
        
        LoanApplication application = applicationRepository.findById(request.getApplicationId())
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        
        if (application.getStatus() == ApplicationStatus.APPROVED) {
            throw new InvalidLoanOperationException("Application already approved");
        }
        
        // Calculate EMI
        EmiCalculationResult emiCalc = emiCalculator.calculateEmi(
            request.getSanctionedAmount(),
            request.getInterestRate(),
            request.getTenureMonths() != null ? 
                request.getTenureMonths() : application.getRequestedTenureMonths(),
            InterestType.REDUCING_BALANCE
        );
        
        // Generate loan number
        String loanNumber = generateLoanNumber();
        
        // Calculate processing fee (1% of loan amount)
        BigDecimal processingFee = request.getSanctionedAmount()
            .multiply(new BigDecimal("0.01"));
        
        // Create loan
        Loan loan = Loan.builder()
            .loanNumber(loanNumber)
            .applicationId(application.getId())
            .userId(application.getUserId())
            .accountId(application.getAccountId())
            .loanType(application.getLoanType())
            .status(LoanStatus.APPROVED)
            .principalAmount(request.getSanctionedAmount())
            .sanctionedAmount(request.getSanctionedAmount())
            .outstandingPrincipal(request.getSanctionedAmount())
            .totalOutstanding(request.getSanctionedAmount())
            .interestRate(request.getInterestRate())
            .tenureMonths(request.getTenureMonths() != null ? 
                request.getTenureMonths() : application.getRequestedTenureMonths())
            .remainingTenureMonths(request.getTenureMonths() != null ? 
                request.getTenureMonths() : application.getRequestedTenureMonths())
            .interestType(InterestType.REDUCING_BALANCE)
            .emiAmount(emiCalc.getEmiAmount())
            .emiDay(5) // 5th of every month
            .totalEmis(request.getTenureMonths() != null ? 
                request.getTenureMonths() : application.getRequestedTenureMonths())
            .processingFee(processingFee)
            .latePaymentPenaltyRate(new BigDecimal("2.0")) // 2% per month
            .prepaymentChargesPercentage(new BigDecimal("2.0")) // 2%
            .loanPurpose(application.getLoanPurpose())
            .creditScore(application.getCreditScore())
            .annualIncome(application.getAnnualIncome())
            .employmentType(application.getEmploymentType())
            .applicationDate(LocalDate.now())
            .approvalDate(LocalDate.now())
            .approvedBy(approvedBy)
            .approvalNotes(request.getApprovalNotes())
            .correlationId(UUID.randomUUID().toString())
            .build();
        
        loan = loanRepository.save(loan);
        
        // Update application
        application.setStatus(ApplicationStatus.APPROVED);
        applicationRepository.save(application);
        
        // Store event
        eventSourcingService.storeEvent(
            loanNumber,
            "LoanApproved",
            buildLoanApprovedEvent(loan),
            loan.getUserId(),
            loan.getCorrelationId(),
            null
        );
        
        // Publish event using Loan Service's outbox
        outboxService.saveEvent("LOAN", loanNumber, "LoanApproved",
            "banking.loan.events", loan);
        
        return mapLoanToResponse(loan);
    }

    @Override
    @Transactional
    public LoanResponse disburseLoan(String loanNumber, Long userId) {
        log.info("Disbursing loan: {}", loanNumber);
        
        Loan loan = findLoanByNumberForUpdate(loanNumber, userId);
        
        if (loan.getStatus() != LoanStatus.APPROVED) {
            throw new InvalidLoanOperationException("Loan is not in approved state");
        }
        
        // Deduct processing fee
        BigDecimal netDisbursement = loan.getSanctionedAmount()
            .subtract(loan.getProcessingFee());
        
        // Credit to account
        accountServiceClient.creditLoanDisbursement(
            loan.getAccountId(),
            netDisbursement,
            loanNumber
        );
        
        // Update loan
        loan.setStatus(LoanStatus.ACTIVE);
        loan.setDisbursedAmount(netDisbursement);
        loan.setDisbursementDate(LocalDate.now());
        loan.setProcessingFeePaid(true);
        
        // Set EMI dates
        LocalDate firstEmiDate = LocalDate.now().plusMonths(1)
            .withDayOfMonth(loan.getEmiDay());
        loan.setFirstEmiDate(firstEmiDate);
        loan.setNextEmiDate(firstEmiDate);
        
        LocalDate maturityDate = firstEmiDate.plusMonths(loan.getTenureMonths() - 1);
        loan.setMaturityDate(maturityDate);
        loan.setLastEmiDate(maturityDate);
        
        loan = loanRepository.save(loan);
        
        // Generate EMI schedule
        generateEmiSchedule(loan);
        
        // Publish event using Loan Service's outbox
        outboxService.saveEvent("LOAN", loanNumber, "LoanDisbursed",
            "banking.loan.events", loan);
        
        log.info("Loan disbursed successfully: {}, amount: {}", loanNumber, netDisbursement);
        
        return mapLoanToResponse(loan);
    }

    @Override
    @Transactional
    public void processEmiPayment(String loanNumber, LocalDate emiDate) {
        Loan loan = loanRepository.findByLoanNumberForUpdate(loanNumber)
            .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));
        
        EmiSchedule emiSchedule = emiScheduleRepository
            .findByLoanIdAndDueDate(loan.getId(), emiDate)
            .orElseThrow(() -> new ResourceNotFoundException("EMI schedule not found"));
        
        if (emiSchedule.getStatus() == EmiStatus.PAID) {
            log.info("EMI already paid: loan={}, date={}", loanNumber, emiDate);
            return;
        }
        
        try {
            // Debit from account
            accountServiceClient.debitEmiPayment(
                loan.getAccountId(),
                emiSchedule.getEmiAmount(),
                loanNumber + "-EMI-" + emiSchedule.getEmiNumber()
            );
            
            // Update EMI schedule
            emiSchedule.setStatus(EmiStatus.PAID);
            emiSchedule.setPaidAmount(emiSchedule.getEmiAmount());
            emiSchedule.setPaidDate(LocalDate.now());
            emiScheduleRepository.save(emiSchedule);
            
            // Update loan
            loan.setOutstandingPrincipal(
                loan.getOutstandingPrincipal().subtract(emiSchedule.getPrincipalComponent())
            );
            loan.setTotalPaid(loan.getTotalPaid().add(emiSchedule.getEmiAmount()));
            loan.setPaidEmis(loan.getPaidEmis() + 1);
            loan.setRemainingTenureMonths(loan.getRemainingTenureMonths() - 1);
            
            // Set next EMI date
            if (loan.getPaidEmis() < loan.getTotalEmis()) {
                loan.setNextEmiDate(emiDate.plusMonths(1));
            } else {
                loan.setStatus(LoanStatus.CLOSED);
                loan.setClosedDate(LocalDate.now());
            }
            
            loanRepository.save(loan);
            
            log.info("EMI payment processed: loan={}, emi={}", loanNumber, 
                emiSchedule.getEmiNumber());
            
        } catch (Exception e) {
            log.error("EMI payment failed: {}", e.getMessage());
            emiSchedule.setStatus(EmiStatus.BOUNCED);
            emiScheduleRepository.save(emiSchedule);
            throw e;
        }
    }

    @Override
    @Transactional
    public PrepaymentResponse prepayLoan(String loanNumber, PrepaymentRequest request, 
                                        Long userId) {
        Loan loan = findLoanByNumberForUpdate(loanNumber, userId);
        
        if (loan.getStatus() != LoanStatus.ACTIVE) {
            throw new InvalidLoanOperationException("Loan is not active");
        }
        
        if (!loan.getPrepaymentAllowed()) {
            throw new InvalidLoanOperationException("Prepayment not allowed for this loan");
        }
        
        BigDecimal prepaymentCharges = emiCalculator.calculatePrepaymentCharges(
            request.getAmount(),
            loan.getPrepaymentChargesPercentage()
        );
        
        BigDecimal totalAmount = request.getAmount().add(prepaymentCharges);
        
        // Debit from account
        accountServiceClient.debitEmiPayment(
            loan.getAccountId(),
            totalAmount,
            loanNumber + "-PREPAY"
        );
        
        // Update loan
        loan.setOutstandingPrincipal(
            loan.getOutstandingPrincipal().subtract(request.getAmount())
        );
        loan.setTotalPaid(loan.getTotalPaid().add(totalAmount));
        
        boolean loanClosed = false;
        
        if (request.getFullPayment() || 
            loan.getOutstandingPrincipal().compareTo(BigDecimal.ZERO) <= 0) {
            // Full prepayment
            loan.setStatus(LoanStatus.FORECLOSED);
            loan.setClosedDate(LocalDate.now());
            loanClosed = true;
        } else {
            // Partial prepayment - recalculate EMI
            EmiCalculationResult newEmi = emiCalculator.calculateEmi(
                loan.getOutstandingPrincipal(),
                loan.getInterestRate(),
                loan.getRemainingTenureMonths(),
                loan.getInterestType()
            );
            loan.setEmiAmount(newEmi.getEmiAmount());
        }
        
        loan = loanRepository.save(loan);
        
        // Publish event using Loan Service's outbox
        outboxService.saveEvent("LOAN", loanNumber, "LoanPrepayment",
            "banking.loan.events", loan);
        
        return PrepaymentResponse.builder()
            .prepaymentAmount(request.getAmount())
            .prepaymentCharges(prepaymentCharges)
            .totalAmount(totalAmount)
            .newOutstandingPrincipal(loan.getOutstandingPrincipal())
            .newEmiAmount(loan.getEmiAmount())
            .newRemainingTenure(loan.getRemainingTenureMonths())
            .loanClosed(loanClosed)
            .build();
    }

    // Helper methods
    private void generateEmiSchedule(Loan loan) {
        List<EmiSchedule> schedules = new ArrayList<>();
        BigDecimal remainingPrincipal = loan.getPrincipalAmount();
        BigDecimal monthlyRate = loan.getInterestRate()
            .divide(new BigDecimal("1200"), 10, java.math.RoundingMode.HALF_UP);
        
        LocalDate emiDate = loan.getFirstEmiDate();
        
        for (int i = 1; i <= loan.getTenureMonths(); i++) {
            BigDecimal interestComponent = remainingPrincipal.multiply(monthlyRate)
                .setScale(2, java.math.RoundingMode.HALF_UP);
            BigDecimal principalComponent = loan.getEmiAmount().subtract(interestComponent);
            
            if (i == loan.getTenureMonths()) {
                // Last EMI adjusts for rounding
                principalComponent = remainingPrincipal;
            }
            
            remainingPrincipal = remainingPrincipal.subtract(principalComponent);
            
            EmiSchedule schedule = EmiSchedule.builder()
                .loanId(loan.getId())
                .emiNumber(i)
                .dueDate(emiDate)
                .emiAmount(loan.getEmiAmount())
                .principalComponent(principalComponent)
                .interestComponent(interestComponent)
                .outstandingPrincipal(remainingPrincipal)
                .status(EmiStatus.SCHEDULED)
                .build();
            
            schedules.add(schedule);
            emiDate = emiDate.plusMonths(1);
        }
        
        emiScheduleRepository.saveAll(schedules);
        log.info("Generated {} EMI schedules for loan: {}", schedules.size(), 
            loan.getLoanNumber());
    }

    private void autoApproveLoan(LoanApplication application, BigDecimal interestRate) {
        LoanApprovalRequest approvalRequest = LoanApprovalRequest.builder()
            .applicationId(application.getId())
            .sanctionedAmount(application.getRequestedAmount())
            .interestRate(interestRate)
            .tenureMonths(application.getRequestedTenureMonths())
            .approvalNotes("Auto-approved based on credit score")
            .build();
        
        approveLoan(approvalRequest, 0L); // System approval
    }

    private BigDecimal calculateMaxEligibleAmount(
            BigDecimal annualIncome, 
            BigDecimal monthlyObligations) {
        
        BigDecimal monthlyIncome = annualIncome.divide(new BigDecimal("12"), 2, 
            java.math.RoundingMode.HALF_UP);
        BigDecimal availableIncome = monthlyIncome.subtract(
            monthlyObligations != null ? monthlyObligations : BigDecimal.ZERO);
        
        // Max EMI = 50% of available income
        BigDecimal maxEmi = availableIncome.multiply(new BigDecimal("0.5"));
        
        // Calculate max loan for 20 years at 12% interest
        // Using reducing balance formula rearranged
        return maxEmi.multiply(new BigDecimal("100")); // Simplified
    }

    private Loan findLoanByNumberForUpdate(String loanNumber, Long userId) {
        Loan loan = loanRepository.findByLoanNumberForUpdate(loanNumber)
            .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));
        
        if (!loan.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("Unauthorized access to loan");
        }
        
        return loan;
    }

    private String generateLoanNumber() {
        return "LOAN-" + System.currentTimeMillis() + "-" + 
               String.format("%04d", new java.util.Random().nextInt(10000));
    }

    private String generateApplicationNumber() {
        return "APP-" + System.currentTimeMillis() + "-" + 
               String.format("%04d", new java.util.Random().nextInt(10000));
    }

    private LoanResponse mapLoanToResponse(Loan loan) {
        return LoanResponse.builder()
            .id(loan.getId())
            .loanNumber(loan.getLoanNumber())
            .userId(loan.getUserId())
            .accountId(loan.getAccountId())
            .loanType(loan.getLoanType())
            .status(loan.getStatus())
            .principalAmount(loan.getPrincipalAmount())
            .outstandingPrincipal(loan.getOutstandingPrincipal())
            .totalOutstanding(loan.getTotalOutstanding())
            .interestRate(loan.getInterestRate())
            .tenureMonths(loan.getTenureMonths())
            .remainingTenureMonths(loan.getRemainingTenureMonths())
            .emiAmount(loan.getEmiAmount())
            .nextEmiDate(loan.getNextEmiDate())
            .paidEmis(loan.getPaidEmis())
            .totalEmis(loan.getTotalEmis())
            .missedEmis(loan.getMissedEmis())
            .disbursementDate(loan.getDisbursementDate())
            .maturityDate(loan.getMaturityDate())
            .createdAt(loan.getCreatedAt())
            .build();
    }

    private LoanApplicationResponse mapApplicationToResponse(LoanApplication app) {
        return LoanApplicationResponse.builder()
            .id(app.getId())
            .applicationNumber(app.getApplicationNumber())
            .loanType(app.getLoanType())
            .status(app.getStatus())
            .requestedAmount(app.getRequestedAmount())
            .requestedTenureMonths(app.getRequestedTenureMonths())
            .creditScore(app.getCreditScore())
            .fraudScore(app.getFraudScore())
            .rejectionReason(app.getRejectionReason())
            .createdAt(app.getCreatedAt())
            .build();
    }

    private Map<String, Object> buildLoanApprovedEvent(Loan loan) {
        return Map.of(
            "loanNumber", loan.getLoanNumber(),
            "userId", loan.getUserId(),
            "amount", loan.getSanctionedAmount(),
            "interestRate", loan.getInterestRate()
        );
    }

    @Override
    @Transactional
    public LoanResponse rejectLoan(LoanRejectionRequest request, Long reviewedBy) {
        log.info("Rejecting loan application: {}", request.getApplicationId());
        
        LoanApplication application = applicationRepository.findById(request.getApplicationId())
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        
        if (application.getStatus() == ApplicationStatus.APPROVED || 
            application.getStatus() == ApplicationStatus.REJECTED) {
            throw new InvalidLoanOperationException("Application already processed");
        }
        
        application.setStatus(ApplicationStatus.REJECTED);
        application.setRejectionReason(request.getRejectionReason());
        application.setReviewedBy(reviewedBy);
        applicationRepository.save(application);
        
        // Publish event using Loan Service's outbox
        outboxService.saveEvent("LOAN_APPLICATION", application.getApplicationNumber(),
            "LoanApplicationRejected", "banking.loan.applications", application);
        
        log.info("Loan application rejected: {}", application.getApplicationNumber());
        
        // Return a dummy LoanResponse since rejection doesn't create a loan
        return LoanResponse.builder()
            .id(application.getId())
            .status(LoanStatus.REJECTED)
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public LoanResponse getLoan(String loanNumber, Long userId) {
        log.info("Fetching loan: {}", loanNumber);
        
        Loan loan = loanRepository.findByLoanNumber(loanNumber)
            .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));
        
        if (!loan.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("Unauthorized access to loan");
        }
        
        return mapLoanToResponse(loan);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LoanResponse> getUserLoans(Long userId, Pageable pageable) {
        log.info("Fetching loans for user: {}", userId);
        
        Page<Loan> loans = loanRepository.findByUserId(userId, pageable);
        return loans.map(this::mapLoanToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmiScheduleResponse> getEmiSchedule(String loanNumber, Long userId) {
        log.info("Fetching EMI schedule for loan: {}", loanNumber);
        
        Loan loan = loanRepository.findByLoanNumber(loanNumber)
            .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));
        
        if (!loan.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("Unauthorized access to loan");
        }
        
        List<EmiSchedule> schedules = emiScheduleRepository.findByLoanId(loan.getId());
        
        return schedules.stream()
            .map(this::mapEmiScheduleToResponse)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public LoanResponse foreCloseLoan(String loanNumber, Long userId) {
        log.info("Foreclosing loan: {}", loanNumber);
        
        Loan loan = findLoanByNumberForUpdate(loanNumber, userId);
        
        if (loan.getStatus() != LoanStatus.ACTIVE && loan.getStatus() != LoanStatus.OVERDUE) {
            throw new InvalidLoanOperationException("Loan cannot be foreclosed in current state");
        }
        
        // Calculate total outstanding including pending EMIs and charges
        BigDecimal totalOutstanding = loan.getOutstandingPrincipal()
            .add(loan.getOutstandingInterest())
            .add(loan.getTotalLatePaymentCharges());
        
        // Calculate prepayment charges
        BigDecimal prepaymentCharges = BigDecimal.ZERO;
        if (loan.getPrepaymentAllowed() && loan.getPrepaymentChargesPercentage() != null) {
            prepaymentCharges = emiCalculator.calculatePrepaymentCharges(
                loan.getOutstandingPrincipal(),
                loan.getPrepaymentChargesPercentage()
            );
        }
        
        BigDecimal totalPayment = totalOutstanding.add(prepaymentCharges);
        
        // Debit from account
        accountServiceClient.debitEmiPayment(
            loan.getAccountId(),
            totalPayment,
            loanNumber + "-FORECLOSE"
        );
        
        // Update loan status
        loan.setStatus(LoanStatus.FORECLOSED);
        loan.setOutstandingPrincipal(BigDecimal.ZERO);
        loan.setOutstandingInterest(BigDecimal.ZERO);
        loan.setTotalOutstanding(BigDecimal.ZERO);
        loan.setClosedDate(LocalDate.now());
        loan.setTotalPaid(loan.getTotalPaid().add(totalPayment));
        
        loan = loanRepository.save(loan);
        
        // Update all pending EMIs to WAIVED
        List<EmiSchedule> pendingEmis = emiScheduleRepository.findByLoanIdAndStatus(
            loan.getId(), EmiStatus.SCHEDULED);
        pendingEmis.forEach(emi -> {
            emi.setStatus(EmiStatus.WAIVED);
            emiScheduleRepository.save(emi);
        });
        
        // Publish event using Loan Service's outbox
        outboxService.saveEvent("LOAN", loanNumber, "LoanForeclosed",
            "banking.loan.events", loan);
        
        log.info("Loan foreclosed successfully: {}, total payment: {}", loanNumber, totalPayment);
        
        return mapLoanToResponse(loan);
    }

    // Helper method to map EMI schedule to response
    private EmiScheduleResponse mapEmiScheduleToResponse(EmiSchedule schedule) {
        return EmiScheduleResponse.builder()
            .id(schedule.getId())
            .emiNumber(schedule.getEmiNumber())
            .dueDate(schedule.getDueDate())
            .emiAmount(schedule.getEmiAmount())
            .principalComponent(schedule.getPrincipalComponent())
            .interestComponent(schedule.getInterestComponent())
            .outstandingPrincipal(schedule.getOutstandingPrincipal())
            .status(schedule.getStatus())
            .paidAmount(schedule.getPaidAmount())
            .paidDate(schedule.getPaidDate())
            .latePaymentCharges(schedule.getLatePaymentCharges())
            .daysOverdue(schedule.getDaysOverdue())
            .build();
    }
    
    @Override
    @Transactional(readOnly = true)
    public LoanSummaryResponse getLoanSummary(Long userId) {
        log.info("Fetching loan summary for user: {}", userId);
        
        Long activeLoansCount = loanRepository.countActiveLoans(userId);
        BigDecimal totalOutstanding = loanRepository.getTotalOutstandingByUser(userId);
        
        List<Loan> activeLoans = loanRepository.findByUserIdAndStatus(
            userId, LoanStatus.ACTIVE, Pageable.unpaged()).getContent();
        
        BigDecimal totalMonthlyEmi = activeLoans.stream()
            .map(Loan::getEmiAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return LoanSummaryResponse.builder()
            .userId(userId)
            .activeLoansCount(activeLoansCount.intValue())
            .totalOutstanding(totalOutstanding != null ? totalOutstanding : BigDecimal.ZERO)
            .totalMonthlyEmi(totalMonthlyEmi)
            .creditUtilization(calculateCreditUtilization(userId, totalOutstanding))
            .build();
    }

    private BigDecimal calculateCreditUtilization(Long userId, BigDecimal totalOutstanding) {
        // max credit limit is 50 lakhs
        BigDecimal maxCreditLimit = new BigDecimal("5000000");
        if (totalOutstanding == null || totalOutstanding.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return totalOutstanding
            .divide(maxCreditLimit, 4, java.math.RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));
    }
}