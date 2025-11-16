package com.payment_service.repository;

import java.math.BigDecimal;

public interface PaymentSummaryProjection {
	Long getTotalCount();

	BigDecimal getTotalAmount();

	BigDecimal getTotalFees();

	BigDecimal getAvgAmount();
}
