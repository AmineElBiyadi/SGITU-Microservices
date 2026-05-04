package com.ensate.billetterie.ticket.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for ticket validation endpoint.
 * Contains the ticket ID, token value, and identity payload for verification.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ValidateTicketRequest {
    
    /**
     * The unique identifier of the ticket to validate
     */
    private String ticketId;
    
    /**
     * The token value (e.g., QR code data, biometric template, face embedding)
     */
    private String tokenValue;
    
    /**
     * The identity verification payload containing biometric/encoded data
     * Key-value pairs specific to the identity method (e.g., QR: decoded data, Fingerprint: hash, Face: embedding)
     */
    private Map<String, Object> identityPayload;
}
