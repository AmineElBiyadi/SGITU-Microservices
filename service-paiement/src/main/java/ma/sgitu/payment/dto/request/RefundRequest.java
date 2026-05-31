package ma.sgitu.payment.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RefundRequest {

    @NotNull(message = "amount est obligatoire")
    @Positive(message = "Le montant doit être positif")
    private BigDecimal amount;

    private String reason;
}