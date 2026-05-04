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
public class TicketRefundRequestedEvent implements BaseEvent {
    @Builder.Default
    private String eventType = "TICKET_REFUND_REQUESTED";
    private String ticketId;
    private String userId;
    private Double montant;
    private String devise;
    private Instant requestedAt;

    @Override
    public Instant getTimestamp() {
        return requestedAt;
    }
}
