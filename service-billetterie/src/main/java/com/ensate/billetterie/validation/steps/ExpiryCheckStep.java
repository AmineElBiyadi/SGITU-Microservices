package com.ensate.billetterie.validation.steps;

import com.ensate.billetterie.ticket.domain.entity.Ticket;
import com.ensate.billetterie.validation.domain.ValidationContext;
import com.ensate.billetterie.validation.exceptions.ValidationException;
import com.ensate.billetterie.validation.interfaces.NextStep;
import com.ensate.billetterie.validation.interfaces.ValidationStep;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Validation step that checks if a ticket has expired.
 * A ticket expires when the current time surpasses its expiresAt timestamp.
 * 
 * Throws ValidationException if the ticket has expired (expiresAt < now).
 */
public class ExpiryCheckStep implements ValidationStep {
    
    private final Executor validationExecutor;
    
    public ExpiryCheckStep(Executor validationExecutor) {
        this.validationExecutor = validationExecutor;
    }
    
    @Override
    public String getStepName() {
        return "ExpiryCheckStep";
    }
    
    @Override
    public CompletableFuture<ValidationContext> execute(
            ValidationContext context,
            NextStep next
    ) {
        return CompletableFuture.supplyAsync(() -> {
            Ticket ticket = context.getResolvedTicket();
            Instant expiresAt = ticket.getExpiresAt();
            Instant now = Instant.now();
            
            // Check if ticket has expired
            if (expiresAt != null && now.isAfter(expiresAt)) {
                throw new ValidationException(
                        getStepName(),
                        "Ticket has expired. Expiry date: " + expiresAt + ", Current time: " + now
                );
            }
            
            return context;
        }, validationExecutor)
        .thenCompose(next::execute)
        .exceptionally(ex -> {
            if (ex.getCause() instanceof ValidationException) {
                throw (ValidationException) ex.getCause();
            }
            throw new ValidationException(
                    getStepName(),
                    "Error checking ticket expiry: " + ex.getMessage(),
                    ex
            );
        });
    }
}
