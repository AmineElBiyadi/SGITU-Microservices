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
public class TicketRefundedEvent implements BaseEvent {
    @Builder.Default
    private String eventType = "TICKET_REFUNDED";
    private String ticketId;
    private String userId;
    private Double montant;
    private String devise;
    private TicketCancelledEvent.Declencheur declencheur;
    private Instant refundedAt;

    @Override
    public Instant getTimestamp() {
        return refundedAt;
    }
}
