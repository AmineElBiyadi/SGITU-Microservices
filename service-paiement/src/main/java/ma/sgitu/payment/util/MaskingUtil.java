package ma.sgitu.payment.util;

/**
 * Utilitaire pour masquer les données sensibles
 */
public class MaskingUtil {

    /**
     * Masque un numéro de carte
     * Exemple : 4532015112830366 → ****0366
     */
    public static String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        return "****" + cardNumber.substring(cardNumber.length() - 4);
    }

    /**
     * Masque un numéro de téléphone
     * Exemple : 0612345678 → 0612****78
     */
    public static String maskPhoneNumber(String phone) {
        if (phone == null || phone.length() < 6) {
            return "****";
        }
        return phone.substring(0, 4) + "****" + phone.substring(phone.length() - 2);
    }
}