package com.ensate.billetterie.ticket.dto.request;

import com.ensate.billetterie.identity.domain.IdentityMethodType;
import com.ensate.billetterie.ticket.domain.enums.TicketClass;
import com.ensate.billetterie.ticket.domain.enums.TicketType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
public class CreateTicketRequest {

    @NotBlank(message = "tripId is required")
    private String tripId;

    @NotBlank(message = "holderId is required")
    private String holderId;

    @Positive(message = "price must be positive")
    private double price;

    @NotBlank(message = "currency is required")
    @Size(min = 3, max = 3, message = "currency must be a 3-character ISO-4217 code")
    private String currency;

    @NotNull(message = "ticketType is required")
    private TicketType ticketType;

    @NotNull(message = "ticketClass is required")
    private TicketClass ticketClass;

    @NotNull(message = "identityMethod is required")
    private IdentityMethodType identityMethod;

    /**
     * Optional raw payload forwarded to IdentityService.
     * Used for biometric methods (e.g. base64-encoded face image).
     */
    private String rawPayload;

    @Future(message = "expiresAt must be in the future")
    private Instant expiresAt;

    private Map<String, Object> metadata;
}
