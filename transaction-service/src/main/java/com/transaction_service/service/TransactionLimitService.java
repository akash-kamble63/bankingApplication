package com.transaction_service.service;

import java.math.BigDecimal;

import com.transaction_service.enums.TransactionType;

public interface TransactionLimitService {
	void checkLimit(Long userId, String accountNumber, TransactionType type, BigDecimal amount);
    void updateLimit(Long userId, String accountNumber, TransactionType type, BigDecimal amount);
    void resetLimits();
}
