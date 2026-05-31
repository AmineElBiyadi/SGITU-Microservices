package ma.sgitu.payment.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO pour appeler G5 Notifications
 * Conforme au contrat G5 v3.1
 *
 * RÈGLE IMPORTANTE :
 * - eventType est au niveau RACINE (pas dans metadata)
 * - metadata contient les données brutes (JAMAIS eventType)
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationRequest {

    // ========== CHAMPS RACINE (Obligatoires) ==========

    /**
     * Identifiant unique de la notification
     * Format : UUID
     * Exemple : "uuid-pay-001"
     */
    private String notificationId;

    /**
     * Service source
     * Valeur fixe pour G6 : "PAYMENT"
     */
    private String sourceService;

    /**
     * Type d'événement - CHAMP RACINE (pas dans metadata)
     * Valeurs possibles pour G6 :
     * - PAYMENT_METHOD_OTP
     * - PAYMENT_SUCCESS
     * - PAYMENT_FAILED
     * - PAYMENT_CANCELLED
     * - INVOICE_GENERATED
     */
    private String eventType;

    /**
     * Canal de notification
     * Valeurs : EMAIL, SMS, PUSH
     * G6 utilise uniquement EMAIL
     */
    private String channel;

    /**
     * Priorité (optionnel)
     * Valeurs : HIGH, NORMAL, LOW
     * Défaut : NORMAL
     */
    private String priority;

    // ========== RECIPIENT (Obligatoire) ==========

    /**
     * Destinataire de la notification
     */
    private Recipient recipient;

    // ========== METADATA (Données brutes) ==========

    /**
     * Données métier spécifiques à l'événement
     * JAMAIS d'eventType ici
     * Exemples :
     * - paymentId
     * - amount
     * - otpCode
     * - invoiceId
     */
    private Map<String, Object> metadata;


    // ========== CLASSE INTERNE : Recipient ==========

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Recipient {

        /**
         * ID utilisateur (obligatoire)
         */
        private String userId;

        /**
         * Email (obligatoire si channel=EMAIL)
         */
        private String email;

        /**
         * Téléphone (obligatoire si channel=SMS)
         */
        private String phone;

        /**
         * Token device (obligatoire si channel=PUSH)
         */
        private String deviceToken;
    }
}