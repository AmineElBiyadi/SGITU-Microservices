package com.ensate.billetterie.validation.steps;

import com.ensate.billetterie.ticket.domain.entity.Ticket;
import com.ensate.billetterie.ticket.repository.TicketRepository;
import com.ensate.billetterie.validation.domain.ValidationContext;
import com.ensate.billetterie.validation.exceptions.ValidationException;
import com.ensate.billetterie.validation.interfaces.NextStep;
import com.ensate.billetterie.validation.interfaces.ValidationStep;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Validation step that checks if a ticket exists in the database.
 * This is the first step in the pipeline - it should be executed before any other checks.
 * 
 * Throws ValidationException if:
 * - Ticket with given ID is not found
 * - Ticket has been soft-deleted
 */
public class TicketExistenceStep implements ValidationStep {
    
    private final TicketRepository ticketRepository;
    private final Executor validationExecutor;
    
    public TicketExistenceStep(TicketRepository ticketRepository, Executor validationExecutor) {
        this.ticketRepository = ticketRepository;
        this.validationExecutor = validationExecutor;
    }
    
    @Override
    public String getStepName() {
        return "TicketExistenceStep";
    }
    
    @Override
    public CompletableFuture<ValidationContext> execute(
            ValidationContext context,
            NextStep next
    ) {
        return CompletableFuture.supplyAsync(() -> {
            // Retrieve ticket from database
            var ticketOptional = ticketRepository.findById(context.getTicketId());
            
            // Check if ticket exists
            if (ticketOptional.isEmpty()) {
                throw new ValidationException(
                        getStepName(),
                        "Ticket with ID '" + context.getTicketId() + "' not found in database"
                );
            }
            
            Ticket ticket = ticketOptional.get();
            
            // Check for soft delete
            if (ticket.getDeletedAt() != null) {
                throw new ValidationException(
                        getStepName(),
                        "Ticket has been deleted"
                );
            }

            // Save the resolved ticket into context to avoid future DB calls
            context.setResolvedTicket(ticket);
            
            return context;
        }, validationExecutor)
        .thenCompose(next::execute)
        .exceptionally(ex -> {
            if (ex.getCause() instanceof ValidationException) {
                throw (ValidationException) ex.getCause();
            }
            throw new ValidationException(
                    getStepName(),
                    "Error checking ticket existence: " + ex.getMessage(),
                    ex
            );
        });
    }
}
