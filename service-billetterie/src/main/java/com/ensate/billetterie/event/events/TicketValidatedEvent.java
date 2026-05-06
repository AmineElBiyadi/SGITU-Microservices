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
public class TicketValidatedEvent implements BaseEvent {
    @Builder.Default
    private String eventType = "TICKET_VALIDATED";
    private String ticketId;
    private String userId;
    private String tripId;
    private String methodeValidation;
    private Instant redeemedAt;

    @Override
    public Instant getTimestamp() {
        return redeemedAt;
    }
}
