package com.ensate.billetterie.validation.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.ensate.billetterie.ticket.domain.entity.Ticket;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ValidationContext {
    
    // Ticket information
    private String ticketId;
    private Ticket resolvedTicket;
    private String holderId;
    private String tokenValue;
    
    // Event information
    private String eventId;
    
    // Identity verification payload
    private Map<String, Object> identityPayload;
    
    // Validation state
    private boolean isDenied;
    private String message;
    private String failedStep;
}
