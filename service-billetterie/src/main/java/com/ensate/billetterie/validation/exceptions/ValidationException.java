package com.ensate.billetterie.validation.exceptions;

/**
 * Exception thrown when a validation step fails.
 * Carries information about which step failed and why.
 */
public class ValidationException extends RuntimeException {
    
    private final String failedStep;
    private final String reason;
    
    /**
     * Constructor with step and reason.
     * @param failedStep The name of the step that failed
     * @param reason The reason for failure
     */
    public ValidationException(String failedStep, String reason) {
        super("Validation failed at step [" + failedStep + "]: " + reason);
        this.failedStep = failedStep;
        this.reason = reason;
    }
    
    /**
     * Constructor with step, reason, and cause.
     * @param failedStep The name of the step that failed
     * @param reason The reason for failure
     * @param cause The underlying cause
     */
    public ValidationException(String failedStep, String reason, Throwable cause) {
        super("Validation failed at step [" + failedStep + "]: " + reason, cause);
        this.failedStep = failedStep;
        this.reason = reason;
    }
    
    public String getFailedStep() {
        return failedStep;
    }
    
    public String getReason() {
        return reason;
    }
}
