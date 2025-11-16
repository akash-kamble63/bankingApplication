package com.payment_service.configuration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.payment_service.entity.Payment;
import com.payment_service.enums.PaymentStatus;
import com.payment_service.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentExpiryJob {
	private final PaymentRepository paymentRepository;

    @Scheduled(fixedDelay = 60000) // Every minute
    @Transactional
    public void expirePendingPayments() {
        List<Payment> expiredPayments = paymentRepository
            .findExpiredPendingPayments(LocalDateTime.now());
        
        if (!expiredPayments.isEmpty()) {
            log.info("Expiring {} pending payments", expiredPayments.size());
            
            for (Payment payment : expiredPayments) {
                payment.setStatus(PaymentStatus.EXPIRED);
                payment.setFailedAt(LocalDateTime.now());
                payment.setFailureReason("Payment timeout - expired after " + 
                    java.time.Duration.between(payment.getCreatedAt(), LocalDateTime.now()).toMinutes() + 
                    " minutes");
                paymentRepository.save(payment);
            }
        }
    }
}
