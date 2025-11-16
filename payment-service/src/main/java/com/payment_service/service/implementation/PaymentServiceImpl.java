package com.payment_service.service.implementation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payment_service.DTOs.BillPaymentRequest;
import com.payment_service.DTOs.CardPaymentRequest;
import com.payment_service.DTOs.PaymentResponse;
import com.payment_service.DTOs.PaymentSagaData;
import com.payment_service.DTOs.SagaResult;
import com.payment_service.DTOs.UpiPaymentRequest;
import com.payment_service.annotation.DistributedLock;
import com.payment_service.entity.Payment;
import com.payment_service.enums.PaymentMethod;
import com.payment_service.enums.PaymentStatus;
import com.payment_service.enums.PaymentType;
import com.payment_service.exception.RateLimitExceededException;
import com.payment_service.exception.ResourceNotFoundException;
import com.payment_service.repository.PaymentRepository;
import com.payment_service.service.EventSourcingService;
import com.payment_service.service.OutboxService;
import com.payment_service.service.PaymentGatewayService;
import com.payment_service.service.PaymentSagaOrchestrator;
import com.payment_service.service.PaymentService;
import com.payment_service.service.RateLimitService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService{
	private final PaymentRepository paymentRepository;
    private final PaymentSagaOrchestrator sagaOrchestrator;
    private final EventSourcingService eventSourcingService;
    private final OutboxService outboxService;
    private final RateLimitService rateLimitService;
    private final PaymentGatewayService gatewayService;
    
    @Transactional
    @DistributedLock(key = "payment:user:#{#request.userId}")
    public PaymentResponse processCardPayment(CardPaymentRequest request, 
                                              Long userId,
                                              String ipAddress,
                                              String userAgent) {
        
        log.info("Processing card payment: userId={}, amount={}", userId, request.getAmount());
        
        // 1. Idempotency check
        if (request.getIdempotencyKey() != null) {
            Optional<Payment> existing = paymentRepository
                .findByIdempotencyKey(request.getIdempotencyKey());
            
            if (existing.isPresent()) {
                log.info("Duplicate request detected, returning cached response");
                return mapToResponse(existing.get());
            }
        }
        
        // 2. Rate limiting check
        if (!rateLimitService.checkPaymentLimit(userId)) {
            throw new RateLimitExceededException("Too many payment attempts");
        }
        
        // 3. Validate card token
        validateCardToken(request.getCardToken());
        
        // 4. Calculate amounts
        BigDecimal taxAmount = calculateTax(request.getAmount());
        BigDecimal feeAmount = calculateFee(request.getAmount(), PaymentMethod.CREDIT_CARD);
        BigDecimal totalAmount = request.getAmount()
            .add(taxAmount)
            .add(feeAmount);
        
        // 5. Generate payment reference
        String paymentRef = generatePaymentReference();
        
        // 6. Create payment (INITIATED)
        Payment payment = Payment.builder()
            .paymentReference(paymentRef)
            .idempotencyKey(request.getIdempotencyKey())
            .userId(userId)
            .accountId(request.getAccountId())
            .merchantId(request.getMerchantId())
            .amount(request.getAmount())
            .taxAmount(taxAmount)
            .feeAmount(feeAmount)
            .totalAmount(totalAmount)
            .currency(request.getCurrency())
            .status(PaymentStatus.INITIATED)
            .paymentMethod(PaymentMethod.CREDIT_CARD)
            .paymentType(PaymentType.CARD_PAYMENT)
            .description(request.getDescription())
            .cardToken(request.getCardToken())
            .cardLastFour(request.getCardLastFour())
            .cardBrand(request.getCardBrand())
            .gatewayName(request.getGatewayName())
            .correlationId(UUID.randomUUID().toString())
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .deviceFingerprint(request.getDeviceFingerprint())
            .expiresAt(LocalDateTime.now().plusMinutes(30))
            .build();
        
        payment = paymentRepository.save(payment);
        
        // 7. Store event (Event Sourcing)
        eventSourcingService.storeEvent(
            paymentRef,
            "PaymentInitiated",
            buildPaymentInitiatedEvent(payment),
            userId,
            payment.getCorrelationId(),
            null
        );
        
        // 8. Execute Payment Saga
        PaymentSagaData sagaData = PaymentSagaData.builder()
            .paymentReference(paymentRef)
            .userId(userId)
            .amount(totalAmount)
            .currency(request.getCurrency())
            .paymentMethod(PaymentMethod.CREDIT_CARD)
            .cardToken(request.getCardToken())
            .gatewayName(request.getGatewayName())
            .merchantId(request.getMerchantId())
            .build();
        
        SagaResult sagaResult = sagaOrchestrator.executeCardPaymentSaga(sagaData);
        
        // 9. Update payment status
        payment = paymentRepository.findByPaymentReference(paymentRef)
            .orElseThrow();
        
        if (sagaResult.isSuccess()) {
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setCompletedAt(LocalDateTime.now());
            payment.setGatewayPaymentId(sagaData.getGatewayPaymentId());
            payment.setExternalTransactionId(sagaData.getExternalTransactionId());
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailedAt(LocalDateTime.now());
            payment.setFailureReason(sagaResult.getErrorMessage());
            payment.setGatewayErrorCode(sagaData.getGatewayErrorCode());
            payment.setGatewayErrorMessage(sagaData.getGatewayErrorMessage());
        }
        
        payment = paymentRepository.save(payment);
        
        // 10. Publish to outbox
        outboxService.saveEvent(
            "PAYMENT",
            paymentRef,
            sagaResult.isSuccess() ? "PaymentCompleted" : "PaymentFailed",
            "banking.payment.status",
            payment
        );
        
        log.info("Card payment processed: {} - Status: {}", paymentRef, payment.getStatus());
        return mapToResponse(payment);
    }
    
    /**
     * Process UPI payment
     */
    @Transactional
    @DistributedLock(key = "payment:user:#{#request.userId}")
    public PaymentResponse processUpiPayment(UpiPaymentRequest request,
                                            Long userId,
                                            String ipAddress) {
        
        log.info("Processing UPI payment: userId={}, upiId={}", userId, request.getUpiId());
        
        // Idempotency check
        if (request.getIdempotencyKey() != null && 
            paymentRepository.existsByIdempotencyKey(request.getIdempotencyKey())) {
            Payment existing = paymentRepository
                .findByIdempotencyKey(request.getIdempotencyKey())
                .orElseThrow();
            return mapToResponse(existing);
        }
        
        // Validate UPI ID
        validateUpiId(request.getUpiId());
        
        // Calculate amounts (UPI has lower fees)
        BigDecimal feeAmount = calculateFee(request.getAmount(), PaymentMethod.UPI);
        BigDecimal totalAmount = request.getAmount().add(feeAmount);
        
        String paymentRef = generatePaymentReference();
        
        Payment payment = Payment.builder()
            .paymentReference(paymentRef)
            .idempotencyKey(request.getIdempotencyKey())
            .userId(userId)
            .accountId(request.getAccountId())
            .amount(request.getAmount())
            .feeAmount(feeAmount)
            .totalAmount(totalAmount)
            .currency("INR")
            .status(PaymentStatus.INITIATED)
            .paymentMethod(PaymentMethod.UPI)
            .paymentType(PaymentType.P2P_TRANSFER)
            .description(request.getDescription())
            .upiId(request.getUpiId())
            .recipientId(request.getRecipientId())
            .recipientName(request.getRecipientName())
            .correlationId(UUID.randomUUID().toString())
            .ipAddress(ipAddress)
            .expiresAt(LocalDateTime.now().plusMinutes(15)) // UPI expires faster
            .build();
        
        payment = paymentRepository.save(payment);
        
        // Execute UPI saga
        PaymentSagaData sagaData = PaymentSagaData.builder()
            .paymentReference(paymentRef)
            .userId(userId)
            .amount(totalAmount)
            .paymentMethod(PaymentMethod.UPI)
            .upiId(request.getUpiId())
            .build();
        
        SagaResult result = sagaOrchestrator.executeUpiPaymentSaga(sagaData);
        
        // Update status
        payment.setStatus(result.isSuccess() ? PaymentStatus.COMPLETED : PaymentStatus.FAILED);
        if (result.isSuccess()) {
            payment.setCompletedAt(LocalDateTime.now());
            payment.setUpiTransactionId(sagaData.getUpiTransactionId());
        } else {
            payment.setFailedAt(LocalDateTime.now());
            payment.setFailureReason(result.getErrorMessage());
        }
        
        payment = paymentRepository.save(payment);
        
        // Publish event
        outboxService.saveEvent("PAYMENT", paymentRef, 
            result.isSuccess() ? "PaymentCompleted" : "PaymentFailed",
            "banking.payment.status", payment);
        
        return mapToResponse(payment);
    }
    
    /**
     * Process bill payment
     */
    @Transactional
    public PaymentResponse processBillPayment(BillPaymentRequest request,
                                             Long userId,
                                             String ipAddress) {
        
        log.info("Processing bill payment: userId={}, billerId={}", userId, request.getBillerId());
        
        // Idempotency
        if (request.getIdempotencyKey() != null &&
            paymentRepository.existsByIdempotencyKey(request.getIdempotencyKey())) {
            return mapToResponse(paymentRepository
                .findByIdempotencyKey(request.getIdempotencyKey())
                .orElseThrow());
        }
        
        BigDecimal feeAmount = calculateFee(request.getAmount(), PaymentMethod.NET_BANKING);
        BigDecimal totalAmount = request.getAmount().add(feeAmount);
        
        String paymentRef = generatePaymentReference();
        
        Payment payment = Payment.builder()
            .paymentReference(paymentRef)
            .idempotencyKey(request.getIdempotencyKey())
            .userId(userId)
            .accountId(request.getAccountId())
            .amount(request.getAmount())
            .feeAmount(feeAmount)
            .totalAmount(totalAmount)
            .currency("INR")
            .status(PaymentStatus.INITIATED)
            .paymentMethod(PaymentMethod.NET_BANKING)
            .paymentType(PaymentType.BILL_PAYMENT)
            .description("Bill Payment: " + request.getBillerName())
            .billerId(request.getBillerId())
            .billNumber(request.getBillNumber())
            .correlationId(UUID.randomUUID().toString())
            .ipAddress(ipAddress)
            .build();
        
        payment = paymentRepository.save(payment);
        
        // Execute bill payment saga
        PaymentSagaData sagaData = PaymentSagaData.builder()
            .paymentReference(paymentRef)
            .userId(userId)
            .amount(totalAmount)
            .paymentMethod(PaymentMethod.NET_BANKING)
            .billerId(request.getBillerId())
            .billNumber(request.getBillNumber())
            .build();
        
        SagaResult result = sagaOrchestrator.executeBillPaymentSaga(sagaData);
        
        payment.setStatus(result.isSuccess() ? PaymentStatus.COMPLETED : PaymentStatus.FAILED);
        if (result.isSuccess()) {
            payment.setCompletedAt(LocalDateTime.now());
        } else {
            payment.setFailedAt(LocalDateTime.now());
            payment.setFailureReason(result.getErrorMessage());
        }
        
        payment = paymentRepository.save(payment);
        
        outboxService.saveEvent("PAYMENT", paymentRef,
            result.isSuccess() ? "PaymentCompleted" : "PaymentFailed",
            "banking.payment.status", payment);
        
        return mapToResponse(payment);
    }
    
    /**
     * Get user payments (NO N+1 queries)
     */
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getUserPayments(Long userId, Pageable pageable) {
        Page<Payment> payments = paymentRepository.findByUserId(userId, pageable);
        return payments.map(this::mapToResponse);
    }
    
    /**
     * Get payment by reference
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByReference(String reference) {
        Payment payment = paymentRepository.findByPaymentReference(reference)
            .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        return mapToResponse(payment);
    }
    
    // Helper methods
    private String generatePaymentReference() {
        return "PAY" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    private BigDecimal calculateTax(BigDecimal amount) {
        // 18% GST
        return amount.multiply(new BigDecimal("0.18")).setScale(2, RoundingMode.HALF_UP);
    }
    
    private BigDecimal calculateFee(BigDecimal amount, PaymentMethod method) {
        BigDecimal feePercentage = switch (method) {
            case CREDIT_CARD -> new BigDecimal("0.025"); // 2.5%
            case DEBIT_CARD -> new BigDecimal("0.01");   // 1%
            case UPI -> new BigDecimal("0.001");         // 0.1%
            case NET_BANKING -> new BigDecimal("0.015"); // 1.5%
            default -> new BigDecimal("0.02");
        };
        
        return amount.multiply(feePercentage).setScale(2, RoundingMode.HALF_UP);
    }
    
    private void validateCardToken(String cardToken) {
        if (cardToken == null || cardToken.length() < 20) {
            throw new IllegalArgumentException("Invalid card token");
        }
    }
    
    private void validateUpiId(String upiId) {
        if (!upiId.matches("^[a-zA-Z0-9.\\-_]+@[a-zA-Z]+$")) {
            throw new IllegalArgumentException("Invalid UPI ID format");
        }
    }
    
    private PaymentResponse mapToResponse(Payment p) {
        return PaymentResponse.builder()
            .id(p.getId())
            .paymentReference(p.getPaymentReference())
            .amount(p.getAmount())
            .taxAmount(p.getTaxAmount())
            .feeAmount(p.getFeeAmount())
            .totalAmount(p.getTotalAmount())
            .currency(p.getCurrency())
            .status(p.getStatus())
            .paymentMethod(p.getPaymentMethod())
            .paymentType(p.getPaymentType())
            .description(p.getDescription())
            .cardLastFour(p.getCardLastFour())
            .cardBrand(p.getCardBrand())
            .upiId(maskUpiId(p.getUpiId()))
            .recipientName(p.getRecipientName())
            .gatewayPaymentId(p.getGatewayPaymentId())
            .fraudScore(p.getFraudScore())
            .riskLevel(p.getRiskLevel())
            .createdAt(p.getCreatedAt())
            .completedAt(p.getCompletedAt())
            .build();
    }
    
    private String maskUpiId(String upiId) {
        if (upiId == null) return null;
        String[] parts = upiId.split("@");
        if (parts.length != 2) return upiId;
        String name = parts[0];
        if (name.length() <= 3) return upiId;
        return name.substring(0, 2) + "***" + name.substring(name.length() - 1) + "@" + parts[1];
    }
    
    private Object buildPaymentInitiatedEvent(Payment payment) {
        return Map.of(
            "paymentReference", payment.getPaymentReference(),
            "amount", payment.getAmount(),
            "userId", payment.getUserId(),
            "paymentMethod", payment.getPaymentMethod()
        );
    }
}
