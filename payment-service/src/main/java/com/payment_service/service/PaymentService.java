package com.payment_service.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.payment_service.DTOs.BillPaymentRequest;
import com.payment_service.DTOs.CardPaymentRequest;
import com.payment_service.DTOs.PaymentResponse;
import com.payment_service.DTOs.UpiPaymentRequest;

public interface PaymentService {
	public PaymentResponse processCardPayment(CardPaymentRequest request, 
            Long userId,
            String ipAddress,
            String userAgent);
	public PaymentResponse processUpiPayment(UpiPaymentRequest request,
            Long userId,
            String ipAddress);
	public PaymentResponse processBillPayment(BillPaymentRequest request,
            Long userId,
            String ipAddress);
	public Page<PaymentResponse> getUserPayments(Long userId, Pageable pageable);
	public PaymentResponse getPaymentByReference(String reference);
	
}
