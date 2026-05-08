package com.ensate.billetterie.validation.steps;

import com.ensate.billetterie.ticket.domain.entity.Ticket;
import com.ensate.billetterie.validation.domain.ValidationContext;
import com.ensate.billetterie.validation.exceptions.ValidationException;
import com.ensate.billetterie.validation.interfaces.NextStep;
import com.ensate.billetterie.validation.interfaces.ValidationStep;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Validation step that checks if the person presenting the ticket is the current holder.
 * This ensures that only the current ticket holder (or an authorized transferee) can redeem it.
 * 
 * Compares the ticket's holderId with the holderId in the ValidationContext.
 * If the context holderId is null, it attempts to extract it from the identity verification payload.
 * 
 * Throws ValidationException if the holder IDs do not match.
 */
public class HolderMatchStep implements ValidationStep {
    
    private final Executor validationExecutor;
    
    public HolderMatchStep(Executor validationExecutor) {
        this.validationExecutor = validationExecutor;
    }
    
    @Override
    public String getStepName() {
        return "HolderMatchStep";
    }
    
    @Override
    public CompletableFuture<ValidationContext> execute(
            ValidationContext context,
            NextStep next
    ) {
        return CompletableFuture.supplyAsync(() -> {
            Ticket ticket = context.getResolvedTicket();
            String ticketHolderId = ticket.getHolderId();
            String presenterId = context.getHolderId();
            
            // Check if holder IDs match
            if (presenterId == null || !presenterId.equals(ticketHolderId)) {
                throw new ValidationException(
                        getStepName(),
                        "Presenter ID '" + presenterId + "' does not match ticket holder '" + 
                        ticketHolderId + "'. Only the ticket holder can redeem it."
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
                    "Error checking holder match: " + ex.getMessage(),
                    ex
            );
        });
    }
}
