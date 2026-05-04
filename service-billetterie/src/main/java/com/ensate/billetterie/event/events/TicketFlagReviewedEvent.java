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
public class TicketFlagReviewedEvent implements BaseEvent {
    @Builder.Default
    private String eventType = "TICKET_FLAG_REVIEWED";
    private String ticketId;
    private String userId;
    private String tripId;
    private String adminId;
    private Decision decision;
    private TicketFlaggedEvent.RaisonFlag raisonFlag;
    private String commentaire;
    private Instant reviewedAt;

    public enum Decision {
        FRAUDE_CONFIRMEE, FAUX_POSITIF
    }

    @Override
    public Instant getTimestamp() {
        return reviewedAt;
    }
}
