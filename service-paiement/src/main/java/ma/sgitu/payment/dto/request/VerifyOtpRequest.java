package ma.sgitu.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour vérifier un code OTP
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VerifyOtpRequest {

    @NotNull(message = "paymentAccountId obligatoire")
    @Positive(message = "paymentAccountId doit être positif")
    private Long paymentAccountId;

    @NotBlank(message = "Code OTP obligatoire")
    private String otpCode;
}