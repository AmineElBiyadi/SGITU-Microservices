package com.ensate.billetterie.event.events;

import com.ensate.billetterie.event.interfaces.BaseEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketIssuedEvent implements BaseEvent {
    @Builder.Default
    private String eventType = "TICKET_ISSUED";
    private String ticketId;
    private String userId;
    private String tripId;
    private String tokenType;
    private String tokenValue;
    private String classe;
    private String type;
    private Instant issuedAt;
    private Instant expiresAt;

    @Override
    public Instant getTimestamp() {
        return issuedAt;
    }
}
