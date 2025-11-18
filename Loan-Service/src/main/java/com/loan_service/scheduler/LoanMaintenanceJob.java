package com.loan_service.scheduler;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loan_service.entity.EmiSchedule;
import com.loan_service.entity.Loan;
import com.loan_service.enums.EmiStatus;
import com.loan_service.enums.LoanStatus;
import com.loan_service.repository.EmiScheduleRepository;
import com.loan_service.repository.LoanRepository;
import com.loan_service.service.LoanService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoanMaintenanceJob {
	private final LoanRepository loanRepository;
    private final EmiScheduleRepository emiScheduleRepository;
    private final LoanService loanService;

    @Scheduled(cron = "0 0 8 * * ?") // Daily at 8 AM
    @Transactional
    public void processScheduledEmis() {
        LocalDate today = LocalDate.now();
        List<EmiSchedule> scheduledEmis = emiScheduleRepository
            .findScheduledEmisForDate(today);
        
        log.info("Processing {} scheduled EMIs for date: {}", scheduledEmis.size(), today);
        
        for (EmiSchedule emi : scheduledEmis) {
            try {
                Loan loan = loanRepository.findById(emi.getLoanId())
                    .orElseThrow();
                
                loanService.processEmiPayment(loan.getLoanNumber(), today);
                
            } catch (Exception e) {
                log.error("EMI auto-debit failed: loanId={}, error={}", 
                    emi.getLoanId(), e.getMessage());
                
                emi.setStatus(EmiStatus.BOUNCED);
                emiScheduleRepository.save(emi);
            }
        }
    }

    @Scheduled(cron = "0 0 9 * * ?") // Daily at 9 AM
    @Transactional
    public void markOverdueEmis() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        List<EmiSchedule> overdueEmis = emiScheduleRepository.findOverdueEmis(yesterday);
        
        if (!overdueEmis.isEmpty()) {
            log.info("Marking {} EMIs as overdue", overdueEmis.size());
            
            for (EmiSchedule emi : overdueEmis) {
                emi.setStatus(EmiStatus.OVERDUE);
                emi.setDaysOverdue(
                    (int) java.time.temporal.ChronoUnit.DAYS.between(
                        emi.getDueDate(), LocalDate.now())
                );
                
                // Calculate late payment charges
                Loan loan = loanRepository.findById(emi.getLoanId()).orElseThrow();
                BigDecimal lateCharges = calculateLateCharges(
                    emi.getEmiAmount(),
                    emi.getDaysOverdue(),
                    loan.getLatePaymentPenaltyRate()
                );
                emi.setLatePaymentCharges(lateCharges);
                
                emiScheduleRepository.save(emi);
                
                // Update loan
                loan.setMissedEmis(loan.getMissedEmis() + 1);
                if (loan.getMissedEmis() >= 1) {
                    loan.setStatus(LoanStatus.OVERDUE);
                }
                loanRepository.save(loan);
            }
        }
    }

    @Scheduled(cron = "0 0 10 * * ?") // Daily at 10 AM
    @Transactional
    public void sendEmiReminders() {
        LocalDate threeDaysLater = LocalDate.now().plusDays(3);
        List<Loan> loansWithUpcomingEmi = loanRepository
            .findLoansWithEmiDueOn(threeDaysLater);
        
        for (Loan loan : loansWithUpcomingEmi) {
            // Send notification via Kafka
            log.info("EMI reminder: loan={}, dueDate={}", 
                loan.getLoanNumber(), loan.getNextEmiDate());
        }
    }

    @Scheduled(cron = "0 0 11 * * ?") // Daily at 11 AM
    @Transactional
    public void flagDefaultedLoans() {
        List<Loan> defaultedLoans = loanRepository.findDefaultedLoans(3);
        
        for (Loan loan : defaultedLoans) {
            if (loan.getStatus() != LoanStatus.DEFAULTED) {
                loan.setStatus(LoanStatus.DEFAULTED);
                loanRepository.save(loan);
                
                log.warn("Loan marked as defaulted: {}", loan.getLoanNumber());
            }
        }
    }

    private BigDecimal calculateLateCharges(BigDecimal emiAmount, Integer daysOverdue,
                                           BigDecimal penaltyRate) {
        return emiAmount
            .multiply(penaltyRate)
            .multiply(new BigDecimal(daysOverdue))
            .divide(new BigDecimal("3000"), 2, java.math.RoundingMode.HALF_UP);
    }
}
