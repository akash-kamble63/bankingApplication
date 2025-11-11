package com.transaction_service.service;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.transaction_service.DTOs.TransactionFilterRequest;
import com.transaction_service.DTOs.TransactionResponse;
import com.transaction_service.DTOs.TransactionSummaryResponse;
import com.transaction_service.DTOs.TransferRequest;

public interface TransactionService {
	public TransactionResponse createTransfer(TransferRequest request, Long userId);

	public Page<TransactionResponse> getUserTransactions(Long userId, Pageable pageable);

	public TransactionSummaryResponse getUserSummary(Long userId, LocalDate date);

	public TransactionResponse getTransactionByReference(String transactionReference);

	public void cancelTransaction(String transactionReference, Long userId);

	public Page<TransactionResponse> searchTransactions(TransactionFilterRequest filter);

	public void updateFraudStatus(String transactionReference, BigDecimal fraudScore, String fraudStatus);

}
