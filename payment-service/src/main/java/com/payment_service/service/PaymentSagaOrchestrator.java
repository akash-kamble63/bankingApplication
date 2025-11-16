package com.payment_service.service;

import com.payment_service.DTOs.PaymentSagaData;
import com.payment_service.DTOs.SagaResult;

public interface PaymentSagaOrchestrator {
	public SagaResult executeCardPaymentSaga(PaymentSagaData data);
	public SagaResult executeUpiPaymentSaga(PaymentSagaData data);
	public SagaResult executeBillPaymentSaga(PaymentSagaData data);
	public void compensateSaga(String sagaId, PaymentSagaData data, Exception error);
	
}
