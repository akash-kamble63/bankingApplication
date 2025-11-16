package com.payment_service.repository;

import java.math.BigDecimal;

public interface PaymentMethodStatistics {
	String getMethod();
    Long getCount();
    BigDecimal getTotal();
}
