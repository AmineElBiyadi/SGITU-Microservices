package ma.sgitu.g5.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Réponse immédiate retournée après réception d'une demande de notification (202 Accepted).
 *
 * Cas ALREADY_QUEUED (idempotence) :
 *   - {@code notificationId}     : l'ID de la notification originale
 *   - {@code originalCreatedAt}  : date de création de la notification originale en base
 *   - {@code currentStatus}      : statut actuel de la notification originale (PENDING/SENT/FAILED)
 *   - {@code status}             : "ALREADY_QUEUED"
 */
@Data
@Schema(description = "Réponse immédiate après réception (202 Accepted)")
public class NotificationResponseDTO {

    @Schema(example = "uuid-g4-001", description = "Identifiant unique de la notification")
    private String notificationId;

    @Schema(example = "QUEUED", description = "QUEUED | ALREADY_QUEUED | ERROR")
    private String status;

    @Schema(example = "Notification prise en charge")
    private String message;

    @Schema(example = "EMAIL")
    private String channel;

    @Schema(example = "2026-05-04T20:30:00", description = "Date/heure d'entrée en file d'attente (nouvelle notif)")
    private String queuedAt;

    // ── Champs supplémentaires pour le cas ALREADY_QUEUED (idempotence) ────────

    @Schema(example = "2026-05-04T18:00:00",
            description = "[ALREADY_QUEUED] Date de création de la notification originale en base")
    private String originalCreatedAt;

    @Schema(example = "SENT",
            description = "[ALREADY_QUEUED] Statut actuel de la notification originale (PENDING/SENT/FAILED)")
    private String currentStatus;

    @Schema(example = "G4_COORDINATION",
            description = "[ALREADY_QUEUED] Service source de la notification originale")
    private String originalSourceService;
}