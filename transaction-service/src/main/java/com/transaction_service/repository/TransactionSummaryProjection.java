package com.transaction_service.repository;

import java.math.BigDecimal;

public interface TransactionSummaryProjection {
	Long getTotalCount();
    BigDecimal getTotalAmount();
    BigDecimal getTotalFees();
}
