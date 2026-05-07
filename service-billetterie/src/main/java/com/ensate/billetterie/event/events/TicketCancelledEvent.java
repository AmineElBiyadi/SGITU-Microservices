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
public class TicketCancelledEvent implements BaseEvent {
    @Builder.Default
    private String eventType = "TICKET_CANCELLED";
    private String ticketId;
    private String userId;
    private String tripId;
    private Declencheur declencheur;
    private Instant cancelledAt;

    public enum Declencheur {
        PASSAGER, ADMIN, SYSTEM
    }

    @Override
    public Instant getTimestamp() {
        return cancelledAt;
    }
}
