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
public class TicketExpiredEvent implements BaseEvent {
    @Builder.Default
    private String eventType = "TICKET_EXPIRED";
    private String ticketId;
    private String userId;
    private String tripId;
    private Instant expiredAt;

    @Override
    public Instant getTimestamp() {
        return expiredAt;
    }
}
