package com.transaction_service.service;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.transaction_service.DTOs.TransactionResponse;
import com.transaction_service.DTOs.TransactionSummaryResponse;
import com.transaction_service.DTOs.TransferRequest;

public interface TransactionService {
	public TransactionResponse createTransfer(TransferRequest request, Long userId);
	public Page<TransactionResponse> getUserTransactions(Long userId, Pageable pageable);
	public TransactionSummaryResponse getUserSummary(Long userId, LocalDate date);
	
}
