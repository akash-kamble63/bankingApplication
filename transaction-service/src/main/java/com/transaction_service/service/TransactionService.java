package com.transaction_service.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.transaction_service.DTO.BillPaymentRequest;
import com.transaction_service.DTO.DepositRequest;
import com.transaction_service.DTO.TransactionFilterRequest;
import com.transaction_service.DTO.TransactionResponse;
import com.transaction_service.DTO.TransactionSummaryResponse;
import com.transaction_service.DTO.TransferRequest;
import com.transaction_service.DTO.WithdrawalRequest;

public interface TransactionService {
	// Core operations
    TransactionResponse deposit(DepositRequest request, String initiatedBy);
    TransactionResponse withdraw(WithdrawalRequest request, String initiatedBy);
    TransactionResponse transfer(TransferRequest request, String initiatedBy);
    TransactionResponse billPayment(BillPaymentRequest request, String initiatedBy);
    
    // Query operations
    TransactionResponse getTransaction(String transactionReference);
    Page<TransactionResponse> getUserTransactions(Long userId, Pageable pageable);
    Page<TransactionResponse> getAccountTransactions(String accountNumber, Pageable pageable);
    Page<TransactionResponse> filterTransactions(TransactionFilterRequest filter);
    
    // Analytics
    TransactionSummaryResponse getAccountSummary(String accountNumber, 
                                                 LocalDateTime start, LocalDateTime end);
    TransactionSummaryResponse getUserSummary(Long userId, 
                                             LocalDateTime start, LocalDateTime end);
    
    // Status operations
    TransactionResponse cancelTransaction(String transactionReference, String reason);
    TransactionResponse reverseTransaction(String transactionReference, String reason);

}
