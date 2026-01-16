package com.transaction_service.service.implementation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.transaction_service.exception.ResourceNotFoundException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.transaction_service.DTOs.SagaResult;
import com.transaction_service.DTOs.TransactionFilterRequest;
import com.transaction_service.DTOs.TransactionResponse;
import com.transaction_service.DTOs.TransactionSummaryResponse;
import com.transaction_service.DTOs.TransferRequest;
import com.transaction_service.DTOs.TransferSagaData;
import com.transaction_service.annotation.DistributedLock;
import com.transaction_service.entity.Transaction;
import com.transaction_service.enums.TransactionStatus;
import com.transaction_service.enums.TransactionType;
import com.transaction_service.patterns.TransactionSagaOrchestrator;
import com.transaction_service.repository.TransactionRepository;
import com.transaction_service.repository.TransactionSummaryProjection;
import com.transaction_service.service.EventSourcingService;
import com.transaction_service.service.OutboxService;
import com.transaction_service.service.TransactionService;
import com.transaction_service.specification.TransactionSpecification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {
    private final TransactionRepository transactionRepository;
    private final TransactionSagaOrchestrator sagaOrchestrator;
    private final EventSourcingService eventSourcingService;
    private final OutboxService outboxService;

    /**
     * Create transfer with ALL patterns:
     * - Distributed Lock (prevents concurrent overdraw)
     * - Idempotency (prevents duplicate submissions)
     * - Saga (distributed transaction with rollback)
     * - Event Sourcing (audit trail)
     * - Outbox (reliable event publishing)
     */
    @Transactional
    @DistributedLock(key = "transfer:#{#request.sourceAccountId}")
    public TransactionResponse createTransfer(TransferRequest request, Long userId) {
        log.info("Creating transfer: {} -> {}",
                request.getSourceAccountId(), request.getDestinationAccountId());

        // 1. Idempotency check
        if (request.getIdempotencyKey() != null) {
            Optional<Transaction> existing = transactionRepository
                    .findByIdempotencyKey(request.getIdempotencyKey());

            if (existing.isPresent()) {
                log.info("Duplicate request detected, returning cached response");
                return mapToResponse(existing.get());
            }
        }

        // 2. Generate unique transaction reference (database sequence)
        String txnRef = generateTransactionReference();

        // 3. Create transaction (INITIATED)
        Transaction transaction = Transaction.builder()
                .transactionReference(txnRef)
                .idempotencyKey(request.getIdempotencyKey())
                .userId(userId)
                .sourceAccountId(request.getSourceAccountId())
                .destinationAccountId(request.getDestinationAccountId())
                .amount(request.getAmount())
                .feeAmount(calculateFee(request.getAmount()))
                .currency(request.getCurrency())
                .status(TransactionStatus.INITIATED)
                .type(TransactionType.TRANSFER)
                .description(request.getDescription())
                .correlationId(UUID.randomUUID().toString())
                .processedBy("SYSTEM")
                .build();

        transaction = transactionRepository.save(transaction);

        // 4. Store event (Event Sourcing)
        eventSourcingService.storeEvent(
                txnRef,
                "TransactionInitiated",
                buildInitiatedEvent(transaction),
                userId,
                transaction.getCorrelationId(),
                null);

        // 5. Execute Saga
        TransferSagaData sagaData = TransferSagaData.builder()
                .transactionReference(txnRef)
                .sourceAccountId(request.getSourceAccountId())
                .destinationAccountId(request.getDestinationAccountId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .userId(userId)
                .build();

        SagaResult sagaResult = sagaOrchestrator.executeTransferSaga(sagaData);

        // 6. Update transaction status
        if (sagaResult.isSuccess()) {
            transaction.setStatus(TransactionStatus.COMPLETED);
            transaction.setCompletedAt(LocalDateTime.now());
        } else {
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason(sagaResult.getErrorMessage());
        }

        transaction = transactionRepository.save(transaction);

        // 7. Publish to outbox
        outboxService.saveEvent(
                "TRANSACTION",
                txnRef,
                sagaResult.isSuccess() ? "TransactionCompleted" : "TransactionFailed",
                "banking.transaction.status",
                transaction);

        log.info("Transfer created: {} - Status: {}", txnRef, transaction.getStatus());
        return mapToResponse(transaction);
    }

    /**
     * Get user transactions
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "userTransactions", key = "#userId + '-' + #pageable.pageNumber")
    public Page<TransactionResponse> getUserTransactions(Long userId, Pageable pageable) {
        Page<Transaction> transactions = transactionRepository
                .findByUserId(userId, pageable);

        return transactions.map(this::mapToResponse);
    }

    /**
     * Get transaction summary
     */
    @Transactional(readOnly = true)
    public TransactionSummaryResponse getUserSummary(Long userId, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        // ✅ Single aggregate query (no loops, no N+1)
        TransactionSummaryProjection summary = transactionRepository
                .getUserTransactionSummary(userId, "COMPLETED", start, end);

        return TransactionSummaryResponse.builder()
                .totalTransactions(summary.getTotalCount())
                .totalAmount(summary.getTotalAmount())
                .totalFees(summary.getTotalFees())
                .build();
    }

    // Helper methods
    private String generateTransactionReference() {
        // Use PostgreSQL sequence for guaranteed uniqueness
        return "TXN" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8);
    }

    private BigDecimal calculateFee(BigDecimal amount) {
        // 0.5% fee
        return amount.multiply(new BigDecimal("0.005")).setScale(2, RoundingMode.HALF_UP);
    }

    private TransactionResponse mapToResponse(Transaction t) {
        return TransactionResponse.builder()
                .id(t.getId())
                .transactionReference(t.getTransactionReference())
                .amount(t.getAmount())
                .feeAmount(t.getFeeAmount())
                .status(t.getStatus())
                .createdAt(t.getCreatedAt())
                .build();
    }

    private Object buildInitiatedEvent(Transaction transaction) {
        return Map.of(
                "transactionReference", transaction.getTransactionReference(),
                "amount", transaction.getAmount(),
                "sourceAccountId", transaction.getSourceAccountId());
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransactionByReference(String transactionReference) {
        Transaction transaction = transactionRepository
                .findByTransactionReference(transactionReference)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        return mapToResponse(transaction);
    }

    @Transactional
    public void cancelTransaction(String transactionReference, Long userId) {
        Transaction transaction = transactionRepository
                .findByTransactionReference(transactionReference)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        // Verify ownership
        if (!transaction.getUserId().equals(userId)) {
            throw new IllegalStateException("Cannot cancel another user's transaction");
        }

        // Can only cancel pending transactions
        if (transaction.getStatus() != TransactionStatus.INITIATED &&
                transaction.getStatus() != TransactionStatus.FRAUD_CHECK_PENDING) {
            throw new IllegalStateException("Cannot cancel transaction in status: " + transaction.getStatus());
        }

        transaction.setStatus(TransactionStatus.CANCELLED);
        transactionRepository.save(transaction);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> searchTransactions(TransactionFilterRequest filter) {
        Specification<Transaction> spec = TransactionSpecification.filterTransactions(filter);

        Sort sort = filter.getSortDirection().equalsIgnoreCase("ASC")
                ? Sort.by(filter.getSortBy()).ascending()
                : Sort.by(filter.getSortBy()).descending();

        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);

        Page<Transaction> transactions = transactionRepository.findAll(spec, pageable);
        return transactions.map(this::mapToResponse);
    }

    @Transactional
    public void updateFraudStatus(String transactionReference,
            BigDecimal fraudScore,
            String fraudStatus) {
        transactionRepository.findByTransactionReference(transactionReference)
                .ifPresent(transaction -> {
                    transaction.setFraudScore(fraudScore);
                    transaction.setFraudStatus(fraudStatus);
                    transactionRepository.save(transaction);

                    log.info("Updated fraud status for transaction: {} - Score: {}",
                            transactionReference, fraudScore);
                });
    }

}
