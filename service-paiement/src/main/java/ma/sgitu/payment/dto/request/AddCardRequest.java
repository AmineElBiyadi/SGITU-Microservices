package ma.sgitu.payment.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour ajouter une carte bancaire
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AddCardRequest {

    @NotNull(message = "userId obligatoire")
    @Positive(message = "userId doit être positif")
    private Long userId;

    @NotBlank(message = "Numéro de carte obligatoire")
    @Pattern(regexp = "^[0-9]{13,19}$", message = "Numéro de carte invalide (13-19 chiffres)")
    private String cardNumber;

    @NotBlank(message = "CVV obligatoire")
    @Pattern(regexp = "^[0-9]{3,4}$", message = "CVV invalide (3 ou 4 chiffres)")
    private String cvv;

    @NotNull(message = "Mois d'expiration obligatoire")
    @Min(value = 1, message = "Mois entre 1 et 12")
    @Max(value = 12, message = "Mois entre 1 et 12")
    private Integer expiryMonth;

    @NotNull(message = "Année d'expiration obligatoire")
    @Min(value = 2026, message = "Année d'expiration invalide")
    private Integer expiryYear;

    @NotBlank(message = "Email obligatoire pour OTP")
    @Email(message = "Format email invalide")
    private String email;
}