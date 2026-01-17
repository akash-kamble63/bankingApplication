package com.account_service.strategy;

public interface SagaCompensator {
    /**
     * Compensate a specific step
     * 
     * @param step             The step name to compensate
     * @param payload          The original saga payload
     * @param compensationData Step-specific compensation data
     * @throws CompensationException if compensation fails
     */
    void compensate(String step, String payload, String compensationData) throws CompensationException;

    /**
     * Get the saga type this compensator handles
     * 
     * @return Saga type identifier
     */
    String sagaType();

    /**
     * Validate if compensation is possible for this step
     * 
     * @param step The step name
     * @return true if step can be compensated
     */
    default boolean canCompensate(String step) {
        return true;
    }

    /**
     * Get compensation priority (lower = execute first during compensation)
     * Useful when multiple compensators need coordination
     * 
     * @return Priority value (default: 100)
     */
    default int compensationPriority() {
        return 100;
    }

    /**
     * Custom exception for compensation failures
     */
    class CompensationException extends Exception {
        private final String step;
        private final boolean retriable;

        public CompensationException(String step, String message, boolean retriable) {
            super(message);
            this.step = step;
            this.retriable = retriable;
        }

        public CompensationException(String step, String message, Throwable cause, boolean retriable) {
            super(message, cause);
            this.step = step;
            this.retriable = retriable;
        }

        public String getStep() {
            return step;
        }

        public boolean isRetriable() {
            return retriable;
        }
    }
}
