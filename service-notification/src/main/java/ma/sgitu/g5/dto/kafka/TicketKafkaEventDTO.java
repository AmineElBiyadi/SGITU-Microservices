package ma.sgitu.g5.dto.kafka;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TicketKafkaEventDTO {
    private String eventType;
    private String ticketId;
    private String userId;
    private String tripId;
    private String tokenType;
    private String tokenValue;
    private String raisonFlag;
    private Double montant;
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;
}
