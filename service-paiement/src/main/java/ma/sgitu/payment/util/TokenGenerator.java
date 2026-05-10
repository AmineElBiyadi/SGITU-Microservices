package ma.sgitu.payment.util;

import java.util.UUID;

/**
 * Générateur de tokens pour moyens de paiement
 */
public class TokenGenerator {

    /**
     * Génère un token pour carte
     * Format : CARD-TOKEN-{uuid}
     * @return String token
     */
    public static String generateCardToken() {
        return "CARD-TOKEN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Génère un token pour Mobile Money
     * Format : MM-TOKEN-{uuid}
     * @return String token
     */
    public static String generateMobileMoneyToken() {
        return "MM-TOKEN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Génère un token de transaction
     * Format : PAY-TOKEN-{timestamp}
     * @return String token
     */
    public static String generateTransactionToken() {
        return "PAY-TOKEN-" + System.currentTimeMillis();
    }
}