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
public class TicketTransferCompletedEvent implements BaseEvent {
    @Builder.Default
    private String eventType = "TICKET_TRANSFER_COMPLETED";
    private String ticketId;
    private String ancienUserId;
    private String nouveauUserId;
    private String tripId;
    private Instant transferredAt;

    @Override
    public Instant getTimestamp() {
        return transferredAt;
    }
}
