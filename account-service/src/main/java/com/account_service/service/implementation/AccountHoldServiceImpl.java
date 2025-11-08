package com.account_service.service.implementation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.account_service.dto.AccountHoldResponse;
import com.account_service.dto.PlaceHoldRequest;
import com.account_service.enums.HoldStatus;
import com.account_service.exception.ResourceNotFoundException;
import com.account_service.model.Account;
import com.account_service.model.AccountHold;
import com.account_service.repository.AccountHoldRepository;
import com.account_service.repository.AccountRepository;
import com.account_service.service.AccountHoldService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountHoldServiceImpl implements AccountHoldService {
    
    private final AccountHoldRepository holdRepository;
    private final AccountRepository accountRepository;
    
    @Override
    @Transactional
    public AccountHoldResponse placeHold(PlaceHoldRequest request) {
        log.info("Placing hold on account: {}", request.getAccountId());
        
        // Verify account exists and has sufficient balance
        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        
        if (account.getAvailableBalance().compareTo(request.getAmount()) < 0) {
            throw new IllegalStateException("Insufficient balance for hold");
        }
        
        // Create hold
        String holdReference = "HOLD-" + UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(request.getExpiryHours());
        
        AccountHold hold = AccountHold.builder()
                .holdReference(holdReference)
                .accountId(request.getAccountId())
                .amount(request.getAmount())
                .status(HoldStatus.ACTIVE)
                .reason(request.getReason())
                .transactionReference(request.getTransactionReference())
                .expiresAt(expiresAt)
                .build();
        
        hold = holdRepository.save(hold);
        
        // Update available balance
        account.setAvailableBalance(account.getAvailableBalance().subtract(request.getAmount()));
        accountRepository.save(account);
        
        log.info("Hold placed: {}", holdReference);
        return mapToResponse(hold);
    }
    
    @Override
    @Transactional
    public AccountHoldResponse releaseHold(String holdReference) {
        log.info("Releasing hold: {}", holdReference);
        
        AccountHold hold = holdRepository.findByHoldReference(holdReference)
                .orElseThrow(() -> new ResourceNotFoundException("Hold not found"));
        
        if (hold.getStatus() != HoldStatus.ACTIVE) {
            throw new IllegalStateException("Hold is not active");
        }
        
        // Update hold status
        hold.setStatus(HoldStatus.RELEASED);
        hold.setReleasedAt(LocalDateTime.now());
        holdRepository.save(hold);
        
        // Restore available balance
        Account account = accountRepository.findById(hold.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        account.setAvailableBalance(account.getAvailableBalance().add(hold.getAmount()));
        accountRepository.save(account);
        
        log.info("Hold released: {}", holdReference);
        return mapToResponse(hold);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<AccountHoldResponse> getAccountHolds(Long accountId) {
        return holdRepository.findByAccountIdAndStatus(accountId, HoldStatus.ACTIVE).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalHolds(Long accountId) {
        return holdRepository.sumActiveHoldsByAccountId(accountId);
    }
    
    @Override
    @Scheduled(fixedDelay = 300000) // Every 5 minutes
    @Transactional
    public void expireOldHolds() {
        LocalDateTime now = LocalDateTime.now();
        List<AccountHold> expiredHolds = holdRepository.findExpiredHolds(now);
        
        log.info("Expiring {} old holds", expiredHolds.size());
        
        for (AccountHold hold : expiredHolds) {
            hold.setStatus(HoldStatus.EXPIRED);
            hold.setReleasedAt(now);
            holdRepository.save(hold);
            
            // Restore available balance
            Account account = accountRepository.findById(hold.getAccountId()).orElse(null);
            if (account != null) {
                account.setAvailableBalance(account.getAvailableBalance().add(hold.getAmount()));
                accountRepository.save(account);
            }
        }
    }
    
    private AccountHoldResponse mapToResponse(AccountHold hold) {
        return AccountHoldResponse.builder()
                .id(hold.getId())
                .holdReference(hold.getHoldReference())
                .accountId(hold.getAccountId())
                .amount(hold.getAmount())
                .status(hold.getStatus())
                .reason(hold.getReason())
                .transactionReference(hold.getTransactionReference())
                .createdAt(hold.getCreatedAt())
                .expiresAt(hold.getExpiresAt())
                .releasedAt(hold.getReleasedAt())
                .build();
    }
}
