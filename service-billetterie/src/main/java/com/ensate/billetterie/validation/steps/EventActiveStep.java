package com.ensate.billetterie.validation.steps;

import com.ensate.billetterie.ticket.client.CoordinationClient;
import com.ensate.billetterie.ticket.domain.entity.Ticket;
import com.ensate.billetterie.ticket.dto.response.MissionDTO;
import com.ensate.billetterie.validation.domain.ValidationContext;
import com.ensate.billetterie.validation.exceptions.ValidationException;
import com.ensate.billetterie.validation.interfaces.NextStep;
import com.ensate.billetterie.validation.interfaces.ValidationStep;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Validation step that checks if the mission associated with the ticket is currently active.
 *
 * IMPORTANT: This step calls the Coordination microservice to retrieve mission data.
 * Mission data is NOT stored locally - it is fetched on-demand from the Coordination Service.
 * This follows the microservices principle: each service owns its data.
 *
 * Throws ValidationException if:
 * - Mission is not found or Coordination Service is unavailable
 * - Mission status is not ONGOING
 * - Mission is outside its entry time window (entry_opens_at / entry_closes_at)
 */
public class EventActiveStep implements ValidationStep {

    private final CoordinationClient coordinationClient;  // Client to call Coordination microservice
    private final Executor validationExecutor;

    public EventActiveStep(CoordinationClient coordinationClient, Executor validationExecutor) {
        this.coordinationClient = coordinationClient;
        this.validationExecutor = validationExecutor;
    }

    @Override
    public String getStepName() {
        return "EventActiveStep";
    }

    @Override
    public CompletableFuture<ValidationContext> execute(
            ValidationContext context,
            NextStep next
    ) {
        return CompletableFuture.supplyAsync(() -> {
            Ticket ticket = context.getResolvedTicket();
            String missionId = ticket.getTripId(); // missionId is stored in tripId field

            // Call Coordination microservice to retrieve mission data
            MissionDTO mission;
            try {
                mission = coordinationClient.getMission(missionId);
            } catch (Exception ex) {
                if (ex instanceof ValidationException) {
                    throw (ValidationException) ex;
                }
                throw new ValidationException(
                        getStepName(),
                        "Could not retrieve mission data from Coordination Service: " + ex.getMessage(),
                        ex
                );
            }

            if (mission == null) {
                throw new ValidationException(
                        getStepName(),
                        "Mission with ID '" + missionId + "' not found"
                );
            }

            // Check if mission is open for entry
            if (!mission.isOpenForEntry()) {
                throw new ValidationException(
                        getStepName(),
                        "Mission '" + mission.getName() + "' is not currently accepting entries. " +
                        "Mission status: " + mission.getStatus() +
                        ", Entry window: " + mission.getEntryOpensAt() + " to " + mission.getEntryClosesAt()
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
                    "Error checking mission status: " + ex.getMessage(),
                    ex
            );
        });
    }
}

