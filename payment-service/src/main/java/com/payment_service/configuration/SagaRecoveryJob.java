package com.payment_service.configuration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.payment_service.entity.SagaState;
import com.payment_service.enums.SagaStatus;
import com.payment_service.repository.SagaStateRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SagaRecoveryJob {
	private final SagaStateRepository sagaRepository;

    @Scheduled(fixedDelay = 60000) // Every minute
    @Transactional
    public void recoverStaleSagas() {
        LocalDateTime timeout = LocalDateTime.now().minusMinutes(30);
        List<SagaState> staleSagas = sagaRepository.findStaleSagas(timeout);
        
        if (!staleSagas.isEmpty()) {
            log.warn("Found {} stale sagas", staleSagas.size());
            
            for (SagaState saga : staleSagas) {
                try {
                    log.error("Marking stale saga as failed: {}", saga.getSagaId());
                    saga.setStatus(SagaStatus.FAILED);
                    saga.setErrorMessage("Saga timeout - exceeded 30 minutes");
                    sagaRepository.save(saga);
                    
                    // Trigger compensation or manual review
                    
                } catch (Exception e) {
                    log.error("Failed to recover saga: {}", saga.getSagaId(), e);
                }
            }
        }
    }
}
