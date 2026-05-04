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
public class TicketTransferInitiatedEvent implements BaseEvent {
    @Builder.Default
    private String eventType = "TICKET_TRANSFER_INITIATED";
    private String ticketId;
    private String userId;
    private String recipientId;
    private String tripId;
    private Instant ttlExpiration;
    private Instant initiatedAt;

    @Override
    public Instant getTimestamp() {
        return initiatedAt;
    }
}
