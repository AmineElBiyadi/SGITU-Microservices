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
public class TicketTransferCancelledEvent implements BaseEvent {
    @Builder.Default
    private String eventType = "TICKET_TRANSFER_CANCELLED";
    private String ticketId;
    private String userId;
    private String recipientId;
    private Raison raison;
    private Instant timestamp;

    public enum Raison {
        REFUSE_PAR_DESTINATAIRE, ANNULE_PAR_EXPEDITEUR, TTL_EXPIRE
    }

    @Override
    public Instant getTimestamp() {
        return timestamp;
    }
}
