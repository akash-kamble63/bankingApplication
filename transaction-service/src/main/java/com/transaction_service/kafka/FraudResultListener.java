package com.transaction_service.kafka;
@Component
@Slf4j
@RequiredArgsConstructor
public class FraudResultListener {
private final TransactionService transactionService;
    
    @KafkaListener(
        topics = "banking.fraud.result",
        groupId = "transaction-service-group",
        concurrency = "3"
    )
    public void handleFraudResult(
            @Payload FraudResultEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
            Acknowledgment acknowledgment) {
        
        try {
            log.info("Received fraud result: txn={}, score={}", 
                event.getTransactionReference(), event.getFraudScore());
            
            transactionService.updateFraudStatus(
                event.getTransactionReference(),
                event.getFraudScore(),
                event.getFraudStatus()
            );
            
            // ✅ Manual acknowledgment (at-least-once processing)
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Error processing fraud result: {}", e.getMessage(), e);
            // Don't acknowledge - will be retried
        }
    }
}
