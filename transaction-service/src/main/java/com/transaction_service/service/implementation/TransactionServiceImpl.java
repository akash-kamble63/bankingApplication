package com.transaction_service.service.implementation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.transaction_service.DTO.AccountResponse;
import com.transaction_service.DTO.BillPaymentRequest;
import com.transaction_service.DTO.DepositRequest;
import com.transaction_service.DTO.TransactionFilterRequest;
import com.transaction_service.DTO.TransactionResponse;
import com.transaction_service.DTO.TransactionSummaryResponse;
import com.transaction_service.DTO.TransferRequest;
import com.transaction_service.DTO.WithdrawalRequest;
import com.transaction_service.entity.Transaction;
import com.transaction_service.enums.AuditAction;
import com.transaction_service.enums.TransactionChannel;
import com.transaction_service.enums.TransactionStatus;
import com.transaction_service.enums.TransactionType;
import com.transaction_service.event.TransactionCompletedEvent;
import com.transaction_service.exceptions.ResourceNotFoundException;
import com.transaction_service.repository.TransactionRepository;
import com.transaction_service.service.AuditService;
import com.transaction_service.service.EventSourcingService;
import com.transaction_service.service.OutboxService;
import com.transaction_service.service.TransactionLimitService;
import com.transaction_service.service.TransactionService;
import com.transaction_service.specification.TransactionSpecification;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService{
	private final TransactionRepository transactionRepository;
    private final EventSourcingService eventSourcingService;
    private final OutboxService outboxService;
    private final AuditService auditService;
    private final TransactionLimitService limitService;
    private final RestTemplate restTemplate;
    
    private static final String ACCOUNT_SERVICE_URL = "http://localhost:8091/api/v1/accounts";
    
    @Override
    @Transactional
    public TransactionResponse deposit(DepositRequest request, String initiatedBy) {
        log.info("Processing deposit: account={}, amount={}", 
                 request.getAccountNumber(), request.getAmount());
        
        // Validate account
        AccountResponse account = getAccountDetails(request.getAccountNumber());
        if (!"ACTIVE".equals(account.getStatus())) {
            throw new IllegalStateException("Account is not active");
        }
        
        // Create transaction
        String txnRef = generateTransactionReference();
        Transaction transaction = Transaction.builder()
                .transactionReference(txnRef)
                .accountNumber(request.getAccountNumber())
                .userId(account.getUserId())
                .transactionType(TransactionType.DEPOSIT)
                .status(TransactionStatus.INITIATED)
                .amount(request.getAmount())
                .currency("INR")
                .description(request.getDescription())
                .paymentMethod(request.getPaymentMethod())
                .openingBalance(account.getBalance())
                .channel(TransactionChannel.WEB.name())
                .initiatedAt(LocalDateTime.now())
                .build();
        
        transaction = transactionRepository.save(transaction);
        
        try {
            // Call Account Service to credit
            creditAccount(request.getAccountNumber(), request.getAmount(), txnRef);
            
            // Update transaction
            transaction.setStatus(TransactionStatus.SUCCESS);
            transaction.setClosingBalance(account.getBalance().add(request.getAmount()));
            transaction.setCompletedAt(LocalDateTime.now());
            transaction = transactionRepository.save(transaction);
            
            // Store event
            TransactionCompletedEvent event = buildCompletedEvent(transaction);
            eventSourcingService.storeEvent(txnRef, "TransactionCompleted", event, 
                                          account.getUserId(), UUID.randomUUID().toString(), null);
            
            // Publish to Kafka via Outbox
            outboxService.saveEvent("TRANSACTION", txnRef, "TransactionCompleted", 
                                   "banking.transaction.completed", event);
            
            // Audit
            auditService.logSuccess(AuditAction.TRANSACTION_COMPLETED, account.getUserId(), 
                                   "TRANSACTION", txnRef, request);
            
            log.info("Deposit successful: {}", txnRef);
            return mapToResponse(transaction);
            
        } catch (Exception e) {
            log.error("Deposit failed: {}", e.getMessage(), e);
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason(e.getMessage());
            transactionRepository.save(transaction);
            
            auditService.logFailure(AuditAction.TRANSACTION_FAILED, account.getUserId(), 
                                   "TRANSACTION", txnRef, request, e.getMessage());
            
            throw new RuntimeException("Deposit failed: " + e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional
    public TransactionResponse withdraw(WithdrawalRequest request, String initiatedBy) {
        log.info("Processing withdrawal: account={}, amount={}", 
                 request.getAccountNumber(), request.getAmount());
        
        AccountResponse account = getAccountDetails(request.getAccountNumber());
        
        // Validate sufficient balance
        if (account.getAvailableBalance().compareTo(request.getAmount()) < 0) {
            throw new IllegalStateException("Insufficient balance");
        }
        
        // Check limits
        limitService.checkLimit(account.getUserId(), request.getAccountNumber(), 
                               TransactionType.WITHDRAWAL, request.getAmount());
        
        String txnRef = generateTransactionReference();
        Transaction transaction = Transaction.builder()
                .transactionReference(txnRef)
                .accountNumber(request.getAccountNumber())
                .userId(account.getUserId())
                .transactionType(TransactionType.WITHDRAWAL)
                .status(TransactionStatus.INITIATED)
                .amount(request.getAmount())
                .currency("INR")
                .description(request.getDescription())
                .paymentMethod(request.getPaymentMethod())
                .location(request.getLocation())
                .openingBalance(account.getBalance())
                .channel(TransactionChannel.WEB.name())
                .initiatedAt(LocalDateTime.now())
                .build();
        
        transaction = transactionRepository.save(transaction);
        
        try {
            // Debit account
            debitAccount(request.getAccountNumber(), request.getAmount(), txnRef);
            
            // Update transaction
            transaction.setStatus(TransactionStatus.SUCCESS);
            transaction.setClosingBalance(account.getBalance().subtract(request.getAmount()));
            transaction.setCompletedAt(LocalDateTime.now());
            transaction = transactionRepository.save(transaction);
            
            // Update limits
            limitService.updateLimit(account.getUserId(), request.getAccountNumber(), 
                                    TransactionType.WITHDRAWAL, request.getAmount());
            
            // Events and audit
            TransactionCompletedEvent event = buildCompletedEvent(transaction);
            eventSourcingService.storeEvent(txnRef, "TransactionCompleted", event, 
                                          account.getUserId(), UUID.randomUUID().toString(), null);
            outboxService.saveEvent("TRANSACTION", txnRef, "TransactionCompleted", 
                                   "banking.transaction.completed", event);
            
            log.info("Withdrawal successful: {}", txnRef);
            return mapToResponse(transaction);
            
        } catch (Exception e) {
            log.error("Withdrawal failed: {}", e.getMessage(), e);
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason(e.getMessage());
            transactionRepository.save(transaction);
            throw new RuntimeException("Withdrawal failed: " + e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional
    public TransactionResponse transfer(TransferRequest request, String initiatedBy) {
        log.info("Processing transfer: from={}, to={}, amount={}", 
                 request.getFromAccount(), request.getToAccount(), request.getAmount());
        
        // This will trigger SAGA orchestration for distributed transfer
        // For now, simplified version
        
        AccountResponse fromAccount = getAccountDetails(request.getFromAccount());
        AccountResponse toAccount = getAccountDetails(request.getToAccount());
        
        // Validate balances
        if (fromAccount.getAvailableBalance().compareTo(request.getAmount()) < 0) {
            throw new IllegalStateException("Insufficient balance");
        }
        
        String txnRef = generateTransactionReference();
        Transaction transaction = Transaction.builder()
                .transactionReference(txnRef)
                .accountNumber(request.getFromAccount())
                .userId(fromAccount.getUserId())
                .transactionType(TransactionType.TRANSFER)
                .status(TransactionStatus.INITIATED)
                .amount(request.getAmount())
                .currency("INR")
                .fromAccount(request.getFromAccount())
                .toAccount(request.getToAccount())
                .description(request.getDescription())
                .paymentMethod(request.getPaymentMethod())
                .upiId(request.getUpiId())
                .openingBalance(fromAccount.getBalance())
                .channel(TransactionChannel.WEB.name())
                .initiatedAt(LocalDateTime.now())
                .build();
        
        transaction = transactionRepository.save(transaction);
        
        try {
            // Debit from source
            debitAccount(request.getFromAccount(), request.getAmount(), txnRef);
            
            // Credit to destination
            creditAccount(request.getToAccount(), request.getAmount(), txnRef);
            
            // Update transaction
            transaction.setStatus(TransactionStatus.SUCCESS);
            transaction.setClosingBalance(fromAccount.getBalance().subtract(request.getAmount()));
            transaction.setCompletedAt(LocalDateTime.now());
            transaction = transactionRepository.save(transaction);
            
            // Events
            TransactionCompletedEvent event = buildCompletedEvent(transaction);
            eventSourcingService.storeEvent(txnRef, "TransactionCompleted", event, 
                                          fromAccount.getUserId(), UUID.randomUUID().toString(), null);
            outboxService.saveEvent("TRANSACTION", txnRef, "TransactionCompleted", 
                                   "banking.transaction.completed", event);
            
            log.info("Transfer successful: {}", txnRef);
            return mapToResponse(transaction);
            
        } catch (Exception e) {
            log.error("Transfer failed: {}", e.getMessage(), e);
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason(e.getMessage());
            transactionRepository.save(transaction);
            throw new RuntimeException("Transfer failed: " + e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(String transactionReference) {
        Transaction transaction = transactionRepository.findByTransactionReference(transactionReference)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
        return mapToResponse(transaction);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> filterTransactions(TransactionFilterRequest filter) {
        Specification<Transaction> spec = TransactionSpecification.filterTransactions(filter);
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), 
                                          Sort.by(Sort.Direction.fromString(filter.getSortDirection()), 
                                                 filter.getSortBy()));
        
        return transactionRepository.findAll(spec, pageable).map(this::mapToResponse);
    }
    
    // Helper methods
    private AccountResponse getAccountDetails(String accountNumber) {
        try {
            String url = ACCOUNT_SERVICE_URL + "/" + accountNumber;
            return restTemplate.getForObject(url, AccountResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch account details", e);
        }
    }
    
    private void creditAccount(String accountNumber, BigDecimal amount, String txnRef) {
        String url = ACCOUNT_SERVICE_URL + "/" + accountNumber + "/credit" +
                    "?amount=" + amount + "&reason=Transaction&transactionRef=" + txnRef;
        restTemplate.postForObject(url, null, Void.class);
    }
    
    private void debitAccount(String accountNumber, BigDecimal amount, String txnRef) {
        String url = ACCOUNT_SERVICE_URL + "/" + accountNumber + "/debit" +
                    "?amount=" + amount + "&reason=Transaction&transactionRef=" + txnRef;
        restTemplate.postForObject(url, null, Void.class);
    }
    
    private String generateTransactionReference() {
        return "TXN-" + UUID.randomUUID().toString();
    }
    
    private TransactionCompletedEvent buildCompletedEvent(Transaction txn) {
        return TransactionCompletedEvent.builder()
                .transactionReference(txn.getTransactionReference())
                .userId(txn.getUserId())
                .accountNumber(txn.getAccountNumber())
                .transactionType(txn.getTransactionType().name())
                .amount(txn.getAmount())
                .status(txn.getStatus().name())
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    private TransactionResponse mapToResponse(Transaction txn) {
        return TransactionResponse.builder()
                .id(txn.getId())
                .transactionReference(txn.getTransactionReference())
                .accountNumber(txn.getAccountNumber())
                .userId(txn.getUserId())
                .transactionType(txn.getTransactionType().name())
                .status(txn.getStatus().name())
                .amount(txn.getAmount())
                .currency(txn.getCurrency())
                .fromAccount(txn.getFromAccount())
                .toAccount(txn.getToAccount())
                .openingBalance(txn.getOpeningBalance())
                .closingBalance(txn.getClosingBalance())
                .description(txn.getDescription())
                .paymentMethod(txn.getPaymentMethod())
                .bankReference(txn.getBankReference())
                .fee(txn.getFee())
                .totalAmount(txn.getTotalAmount())
                .channel(txn.getChannel())
                .failureReason(txn.getFailureReason())
                .createdAt(txn.getCreatedAt())
                .completedAt(txn.getCompletedAt())
                .build();
    }

    @Override
    public TransactionResponse billPayment(BillPaymentRequest request, String initiatedBy) {
        // Implementation similar to withdrawal
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Page<TransactionResponse> getUserTransactions(Long userId, Pageable pageable) {
        return transactionRepository.findByUserId(userId, pageable).map(this::mapToResponse);
    }

    @Override
    public Page<TransactionResponse> getAccountTransactions(String accountNumber, Pageable pageable) {
        return transactionRepository.findByAccountNumber(accountNumber, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public TransactionSummaryResponse getAccountSummary(String accountNumber, 
                                                        LocalDateTime start, LocalDateTime end) {
        // Implement summary calculations
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public TransactionSummaryResponse getUserSummary(Long userId, 
                                                     LocalDateTime start, LocalDateTime end) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public TransactionResponse cancelTransaction(String transactionReference, String reason) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public TransactionResponse reverseTransaction(String transactionReference, String reason) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
