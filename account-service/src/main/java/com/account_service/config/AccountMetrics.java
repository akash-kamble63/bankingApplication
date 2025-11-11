package com.account_service.config;
import org.springframework.stereotype.Component;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
@Component
@RequiredArgsConstructor
public class AccountMetrics {
private final MeterRegistry meterRegistry;
    
    public void recordBalanceUpdate(String operation, boolean success) {
        Counter.builder("account.balance.updates")
            .tag("operation", operation)
            .tag("success", String.valueOf(success))
            .register(meterRegistry)
            .increment();
    }
    
    public void recordConcurrencyConflict(String operation) {
        Counter.builder("account.concurrency.conflicts")
            .tag("operation", operation)
            .register(meterRegistry)
            .increment();
    }
    
    public void recordAccountCreation(String accountType) {
        Counter.builder("account.creations")
            .tag("type", accountType)
            .register(meterRegistry)
            .increment();
    }
}
