package ma.sgitu.g5.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Résultat interne retourné par un provider")
public class SendResultDTO {

    @Schema(example = "true")
    private boolean success;

    @Schema(example = "SENDGRID", description = "TWILIO | SENDGRID | FCM")
    private String provider;

    @Schema(example = "QUOTA_EXCEEDED")
    private String errorCode;

    @Schema(example = "0")
    private int retryCount;
}