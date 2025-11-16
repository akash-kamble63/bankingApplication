package com.payment_service.controller;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.payment_service.DTOs.BillPaymentRequest;
import com.payment_service.DTOs.CardPaymentRequest;
import com.payment_service.DTOs.PaymentResponse;
import com.payment_service.DTOs.UpiPaymentRequest;
import com.payment_service.service.PaymentService;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payment Management", description = "Payment processing endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class PaymentController {
	private final PaymentService paymentService;

    @PostMapping("/card")
    @PreAuthorize("hasRole('USER')")
    @Timed(value = "payment.card.process", description = "Time to process card payment")
    @Operation(summary = "Process card payment", description = "Process credit/debit card payment with fraud detection and saga orchestration")
    public ResponseEntity<PaymentResponse> processCardPayment(
            @Valid @RequestBody CardPaymentRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        
        Long userId = extractUserId(authentication);
        String ipAddress = extractIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        
        log.info("Card payment request received: userId={}, amount={}", userId, request.getAmount());
        
        PaymentResponse response = paymentService.processCardPayment(
            request, userId, ipAddress, userAgent
        );
        
        return ResponseEntity
            .status(response.getStatus().toString().equals("COMPLETED") ? 
                HttpStatus.CREATED : HttpStatus.ACCEPTED)
            .body(response);
    }

    @PostMapping("/upi")
    @PreAuthorize("hasRole('USER')")
    @Timed(value = "payment.upi.process", description = "Time to process UPI payment")
    @Operation(summary = "Process UPI payment", description = "Process UPI payment with real-time status check")
    public ResponseEntity<PaymentResponse> processUpiPayment(
            @Valid @RequestBody UpiPaymentRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        
        Long userId = extractUserId(authentication);
        String ipAddress = extractIpAddress(httpRequest);
        
        log.info("UPI payment request: userId={}, upiId={}", userId, request.getUpiId());
        
        PaymentResponse response = paymentService.processUpiPayment(
            request, userId, ipAddress
        );
        
        return ResponseEntity
            .status(HttpStatus.ACCEPTED)
            .body(response);
    }

    @PostMapping("/bill")
    @PreAuthorize("hasRole('USER')")
    @Timed(value = "payment.bill.process", description = "Time to process bill payment")
    @Operation(summary = "Process bill payment", description = "Process utility bill payment")
    public ResponseEntity<PaymentResponse> processBillPayment(
            @Valid @RequestBody BillPaymentRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        
        Long userId = extractUserId(authentication);
        String ipAddress = extractIpAddress(httpRequest);
        
        log.info("Bill payment request: userId={}, billerId={}", userId, request.getBillerId());
        
        PaymentResponse response = paymentService.processBillPayment(
            request, userId, ipAddress
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get user payments", description = "Get paginated list of user's payments")
    public ResponseEntity<Page<PaymentResponse>> getUserPayments(
            Authentication authentication,
            Pageable pageable) {
        
        Long userId = extractUserId(authentication);
        Page<PaymentResponse> payments = paymentService.getUserPayments(userId, pageable);
        
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/{reference}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get payment by reference", description = "Get payment details by payment reference")
    public ResponseEntity<PaymentResponse> getPaymentByReference(
            @PathVariable String reference,
            Authentication authentication) {
        
        Long userId = extractUserId(authentication);
        PaymentResponse payment = paymentService.getPaymentByReference(reference);
        
        // Verify ownership
        if (!payment.getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(payment);
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check payment service health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Payment Service is healthy");
    }

    // Helper methods
    private Long extractUserId(Authentication authentication) {
        // Extract from JWT claims
        return Long.parseLong(authentication.getName());
    }

    private String extractIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
