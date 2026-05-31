package ma.sgitu.g5.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Coordonnées du destinataire")
public class RecipientDTO {

    @NotBlank(message = "userId est obligatoire")
    @Schema(example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String userId;

    @Email(message = "Format email invalide")
    @Schema(example = "client@example.com")
    private String email;

    @Schema(example = "+212600000000")
    private String phone;

    @Schema(example = "fcm-token-abc")
    private String deviceToken;
}
