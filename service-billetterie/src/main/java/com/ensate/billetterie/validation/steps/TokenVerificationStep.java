package com.ensate.billetterie.validation.steps;

import com.ensate.billetterie.identity.domain.IdentityToken;
import com.ensate.billetterie.identity.dto.VerifyTokenRequest;
import com.ensate.billetterie.identity.service.IdentityService;
import com.ensate.billetterie.ticket.domain.entity.Ticket;
import com.ensate.billetterie.validation.domain.ValidationContext;
import com.ensate.billetterie.validation.exceptions.ValidationException;
import com.ensate.billetterie.validation.interfaces.NextStep;
import com.ensate.billetterie.validation.interfaces.ValidationStep;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Validation step that verifies the authenticity of the token presented.
 * This is the final step before a ticket is considered successfully validated.
 * 
 * Delegates to IdentityService to verify the token using the appropriate identity method
 * (QR code, fingerprint, face ID, etc.).
 * 
 * NOTE ON ARCHITECTURE:
 * IdentityService is currently implemented as an internal module within this service.
 * If the identity domain is extracted into its own separate microservice in the future,
 * this class will need to be updated to use an IdentityClient HTTP client 
 * (similar to how CoordinationClient is used) instead of a direct service reference.
 * 
 * Throws ValidationException if:
 * - Token is missing or malformed
 * - Token verification fails
 * - Identity service encounters an error
 */
public class TokenVerificationStep implements ValidationStep {
    
    private final IdentityService identityService;
    private final Executor validationExecutor;
    
    public TokenVerificationStep(IdentityService identityService, Executor validationExecutor) {
        this.identityService = identityService;
        this.validationExecutor = validationExecutor;
    }
    
    @Override
    public String getStepName() {
        return "TokenVerificationStep";
    }
    
    @Override
    public CompletableFuture<ValidationContext> execute(
            ValidationContext context,
            NextStep next
    ) {
        return CompletableFuture.supplyAsync(() -> {
            Ticket ticket = context.getResolvedTicket();
            
            // Validate that token value is present in context
            if (context.getTokenValue() == null || context.getTokenValue().isEmpty()) {
                throw new ValidationException(
                        getStepName(),
                        "Token value is missing from validation request"
                );
            }
            
            // Build IdentityToken from stored ticket information
            IdentityToken storedToken = IdentityToken.builder()
                    .tokenValue(ticket.getTokenValue())
                    .methodType(ticket.getIdentityMethod())
                    .build();
            
            // Build VerifyTokenRequest
            VerifyTokenRequest verifyRequest = VerifyTokenRequest.builder()
                    .token(storedToken)
                    .holderId(context.getHolderId())
                    .eventId(context.getEventId())
                    .rawPayload(context.getIdentityPayload())
                    .build();
            
            // Call IdentityService to verify token
            boolean isValid = identityService.verify(verifyRequest);
            
            if (!isValid) {
                throw new ValidationException(
                        getStepName(),
                        "Token verification failed. The presented token does not match the ticket's identity method (" +
                        ticket.getIdentityMethod() + ")."
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
                    "Error during token verification: " + ex.getMessage(),
                    ex
            );
        });
    }
}
