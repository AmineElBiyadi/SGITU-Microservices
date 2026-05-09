package com.ensate.billetterie.ticket.dto.response;

import com.ensate.billetterie.ticket.domain.enums.TicketStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class AuditEntryResponse {

    /** Sequential position of this entry in the ticket's audit trail. */
    private int sequence;

    /** Status the ticket transitioned FROM (null for the first entry). */
    private TicketStatus fromStatus;

    /** Status the ticket transitioned TO. */
    private TicketStatus toStatus;

    /** Human-readable description of the action (e.g. "Ticket validated at gate A3"). */
    private String action;

    /** ID of the user or system actor that triggered this transition. */
    private String performedBy;

    /** When this transition occurred. */
    private Instant occurredAt;

    /** Free-text reason or admin note associated with this transition. */
    private String reason;

    /** Optional extra context (IP address, device ID, location, etc.). */
    private Map<String, Object> context;
}
