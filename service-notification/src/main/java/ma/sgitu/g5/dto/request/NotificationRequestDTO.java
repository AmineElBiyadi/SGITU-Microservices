package ma.sgitu.g5.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "Payload unique pour l'envoi de notification (tous les groupes REST)")
public class NotificationRequestDTO {

    @NotBlank(message = "notificationId est obligatoire (UUID)")
    @Schema(example = "uuid-g4-001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String notificationId;

    @NotBlank(message = "sourceService est obligatoire")
    @Pattern(regexp = "G[1-9]|G10_.*|G[1-9]_.*", message = "Format attendu : G4_COORDINATION, G6_PAYMENT...")
    @Schema(example = "G4_COORDINATION", requiredMode = Schema.RequiredMode.REQUIRED)
    private String sourceService;

    @NotBlank(message = "eventType est obligatoire")
    @Schema(example = "MISSION_CANCELLED", requiredMode = Schema.RequiredMode.REQUIRED)
    private String eventType;

    @NotBlank(message = "channel est obligatoire")
    @Pattern(regexp = "EMAIL|SMS|PUSH|LOG", message = "Canal doit être EMAIL, SMS, PUSH ou LOG")
    @Schema(example = "EMAIL", requiredMode = Schema.RequiredMode.REQUIRED)
    private String channel;

    @NotBlank(message = "priority est obligatoire")
    @Pattern(regexp = "HIGH|NORMAL|LOW", message = "Priorité doit être HIGH, NORMAL ou LOW")
    @Schema(example = "HIGH", requiredMode = Schema.RequiredMode.REQUIRED)
    private String priority;

    @NotNull(message = "recipient est obligatoire")
    @Valid
    private RecipientDTO recipient;

    @Valid
    private MetadataDTO metadata;
}