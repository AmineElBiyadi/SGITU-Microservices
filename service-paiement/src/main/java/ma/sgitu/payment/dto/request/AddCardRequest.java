package ma.sgitu.payment.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class AddCardRequest {

    @NotNull
    private Long userId;

    @NotBlank
    private String cardNumber;

    @NotBlank
    private String cvv;

    @NotNull
    @Min(1) @Max(12)
    private Integer expiryMonth;

    @NotNull
    private Integer expiryYear;

    @NotBlank
    private String email; // pour envoyer l'OTP via G5
}