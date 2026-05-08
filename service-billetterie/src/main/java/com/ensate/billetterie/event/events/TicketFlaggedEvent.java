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
public class TicketFlaggedEvent implements BaseEvent {
    @Builder.Default
    private String eventType = "TICKET_FLAGGED";
    private String ticketId;
    private String userId;
    private String tripId;
    private RaisonFlag raisonFlag;
    private Instant flaggedAt;

    public enum RaisonFlag {
        TOKEN_INVALIDE, TICKET_DEJA_UTILISE, MAUVAIS_TRIP, HORS_HORAIRE
    }

    @Override
    public Instant getTimestamp() {
        return flaggedAt;
    }
}
