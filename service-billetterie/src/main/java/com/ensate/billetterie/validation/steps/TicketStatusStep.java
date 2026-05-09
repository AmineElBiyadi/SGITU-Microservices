package com.ensate.billetterie.validation.steps;

import com.ensate.billetterie.ticket.domain.entity.Ticket;
import com.ensate.billetterie.ticket.domain.enums.TicketStatus;
import com.ensate.billetterie.validation.domain.ValidationContext;
import com.ensate.billetterie.validation.exceptions.ValidationException;
import com.ensate.billetterie.validation.interfaces.NextStep;
import com.ensate.billetterie.validation.interfaces.ValidationStep;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Validation step that checks if a ticket has a valid status for validation.
 * Only tickets with status ISSUED can be validated and redeemed.
 * 
 * Throws ValidationException if ticket status is:
 * - TRANSFERRED (transferred to another holder)
 * - REDEEMED or USED (already used)
 * - CANCELLED (cancelled by owner or admin)
 * - EXPIRED (passed expiry date)
 * - REFUNDED (refunded to holder)
 * - FLAGGED (flagged for review)
 */
public class TicketStatusStep implements ValidationStep {
    
    private final Executor validationExecutor;
    
    public TicketStatusStep(Executor validationExecutor) {
        this.validationExecutor = validationExecutor;
    }
    
    @Override
    public String getStepName() {
        return "TicketStatusStep";
    }
    
    @Override
    public CompletableFuture<ValidationContext> execute(
            ValidationContext context,
            NextStep next
    ) {
        return CompletableFuture.supplyAsync(() -> {
            Ticket ticket = context.getResolvedTicket();
            TicketStatus status = ticket.getStatus();
            
            // Only ISSUED status is valid for validation
            if (status != TicketStatus.ISSUED) {
                throw new ValidationException(
                        getStepName(),
                        "Ticket status '" + status + "' is not eligible for validation. " +
                        "Only ISSUED tickets can be validated."
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
                    "Error checking ticket status: " + ex.getMessage(),
                    ex
            );
        });
    }
}

