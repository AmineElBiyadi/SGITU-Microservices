package ma.sgitu.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO de réponse pour cartes de test
 * Ne contient AUCUNE donnée sensible
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TestCardResponse {

    private Long id;
    private String last4;
    private String cardHolderName;
    private Integer expiryMonth;
    private Integer expiryYear;
    private String provider;
    private BigDecimal balance;
    private String status;
}