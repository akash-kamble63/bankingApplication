package com.transaction_service.service.implementation;


import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.transaction_service.entity.TransactionLimit;
import com.transaction_service.enums.LimitType;
import com.transaction_service.enums.TransactionType;
import com.transaction_service.repository.TransactionLimitRepository;
import com.transaction_service.service.TransactionLimitService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionLimitServiceImpl implements TransactionLimitService{
	
private final TransactionLimitRepository limitRepository;
    
    private static final BigDecimal DAILY_WITHDRAWAL_LIMIT = new BigDecimal("100000.00");
    private static final BigDecimal DAILY_TRANSFER_LIMIT = new BigDecimal("200000.00");
    private static final BigDecimal TRANSACTION_LIMIT = new BigDecimal("50000.00");
    
    @Override
    @Transactional
    public void checkLimit(Long userId, String accountNumber, TransactionType type, BigDecimal amount) {
        log.debug("Checking limits for user: {}, type: {}, amount: {}", userId, type, amount);
        
        // Check transaction limit
        if (amount.compareTo(TRANSACTION_LIMIT) > 0) {
            throw new IllegalStateException("Transaction amount exceeds limit: " + TRANSACTION_LIMIT);
        }
        
        // Check daily limit
        TransactionLimit dailyLimit = getOrCreateLimit(userId, accountNumber, type, LimitType.DAILY);
        
        if (dailyLimit.getCurrentAmount().add(amount).compareTo(dailyLimit.getMaxAmount()) > 0) {
            throw new IllegalStateException("Daily limit exceeded. Remaining: " + 
                    dailyLimit.getMaxAmount().subtract(dailyLimit.getCurrentAmount()));
        }
    }
    
    @Override
    @Transactional
    public void updateLimit(Long userId, String accountNumber, TransactionType type, BigDecimal amount) {
        log.debug("Updating limits for user: {}, type: {}, amount: {}", userId, type, amount);
        
        TransactionLimit dailyLimit = getOrCreateLimit(userId, accountNumber, type, LimitType.DAILY);
        dailyLimit.setCurrentAmount(dailyLimit.getCurrentAmount().add(amount));
        dailyLimit.setCurrentCount(dailyLimit.getCurrentCount() + 1);
        limitRepository.save(dailyLimit);
    }
    
    @Override
    @Transactional
    public void resetLimits() {
        log.info("Resetting daily transaction limits");
        
        LocalDateTime now = LocalDateTime.now();
        limitRepository.findAll().forEach(limit -> {
            if (limit.getLimitType() == LimitType.DAILY && 
                limit.getResetAt() != null && 
                now.isAfter(limit.getResetAt())) {
                
                limit.setCurrentAmount(BigDecimal.ZERO);
                limit.setCurrentCount(0);
                limit.setResetAt(now.plusDays(1).withHour(0).withMinute(0).withSecond(0));
                limitRepository.save(limit);
            }
        });
    }
    
    private TransactionLimit getOrCreateLimit(Long userId, String accountNumber, 
                                             TransactionType type, LimitType limitType) {
        // Simplified - you should add accountId lookup
        return limitRepository.findByUserIdAndTransactionTypeAndLimitType(userId, type, limitType)
                .orElseGet(() -> {
                    BigDecimal maxAmount = type == TransactionType.WITHDRAWAL 
                            ? DAILY_WITHDRAWAL_LIMIT 
                            : DAILY_TRANSFER_LIMIT;
                    
                    TransactionLimit limit = TransactionLimit.builder()
                            .userId(userId)
                            .transactionType(type)
                            .limitType(limitType)
                            .maxAmount(maxAmount)
                            .maxCount(100)
                            .currentAmount(BigDecimal.ZERO)
                            .currentCount(0)
                            .resetAt(LocalDateTime.now().plusDays(1).withHour(0).withMinute(0).withSecond(0))
                            .build();
                    
                    return limitRepository.save(limit);
                });
    }
}
