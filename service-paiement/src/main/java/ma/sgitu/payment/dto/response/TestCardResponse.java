package ma.sgitu.payment.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TestCardResponse {

    private Long id;
    private String last4;
    private String cardHolderName;
    private Integer expiryMonth;
    private Integer expiryYear;
    private String provider;
    private BigDecimal balance;
    private String status;
    // Pas de cardNumberHash, pas de cvvHash → sécurité
}