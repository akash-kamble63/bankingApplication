package com.loan_service.client;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountServiceClient {
private final WebClient accountServiceWebClient;
    
    public boolean validateAccountOwnership(Long accountId, Long userId) {
        return true;
    }
    
    public void creditLoanDisbursement(Long accountId, BigDecimal amount, String reference) {
        // Credit account
    }
    
    public void debitEmiPayment(Long accountId, BigDecimal amount, String reference) {
        // Debit account
    }
}
