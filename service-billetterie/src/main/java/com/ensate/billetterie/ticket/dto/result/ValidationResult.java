package com.ensate.billetterie.ticket.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for validation result.
 * Contains the validation success status, and details if validation failed.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ValidationResult {
    
    /**
     * true if the ticket validation succeeded, false otherwise
     */
    private boolean valid;
    
    /**
     * Name of the validation step that failed (null if validation succeeded)
     */
    private String failedStep;
    
    /**
     * Human-readable reason for validation failure (null if validation succeeded)
     */
    private String reason;
    
    /**
     * Convenience method to create a successful validation result
     */
    public static ValidationResult success() {
        return ValidationResult.builder()
                .valid(true)
                .build();
    }
    
    /**
     * Convenience method to create a failed validation result
     */
    public static ValidationResult failure(String failedStep, String reason) {
        return ValidationResult.builder()
                .valid(false)
                .failedStep(failedStep)
                .reason(reason)
                .build();
    }
}
