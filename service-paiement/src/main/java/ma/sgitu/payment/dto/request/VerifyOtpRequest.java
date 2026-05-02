package ma.sgitu.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class VerifyOtpRequest {

    @NotNull
    private Long userId;

    @NotBlank
    private String otp;
}