package com.transaction_service.monitor;

import java.time.Duration;

import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class TransactionMetrics {
private final MeterRegistry meterRegistry;
    
    @PostConstruct
    public void init() {
        // Custom metrics
        Counter.builder("transactions.created")
            .description("Total transactions created")
            .tag("service", "transaction")
            .register(meterRegistry);
        
        Counter.builder("transactions.failed")
            .description("Total transactions failed")
            .tag("service", "transaction")
            .register(meterRegistry);
        
        Timer.builder("transactions.processing.time")
            .description("Transaction processing time")
            .register(meterRegistry);
    }
    
    public void recordTransactionCreated() {
        meterRegistry.counter("transactions.created").increment();
    }
    
    public void recordTransactionFailed(String reason) {
        meterRegistry.counter("transactions.failed", "reason", reason).increment();
    }
    
    public void recordProcessingTime(Duration duration) {
        meterRegistry.timer("transactions.processing.time")
            .record(duration);
    }
}
