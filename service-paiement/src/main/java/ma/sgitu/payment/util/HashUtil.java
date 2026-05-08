package ma.sgitu.payment.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utilitaire pour hasher les données sensibles
 * Utilise BCrypt pour sécurité maximale
 */
public class HashUtil {

    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /**
     * Hash une chaîne de caractères avec BCrypt
     *
     * @param input Donnée à hasher (carte, CVV, téléphone, OTP)
     * @return String hashée
     */
    public static String hash(String input) {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("Input ne peut pas être vide");
        }
        return encoder.encode(input);
    }

    /**
     * Vérifie si une donnée correspond au hash
     *
     * @param input Donnée en clair
     * @param hash Hash à comparer
     * @return boolean
     */
    public static boolean verify(String input, String hash) {
        if (input == null || hash == null) {
            return false;
        }
        return encoder.matches(input, hash);
    }
}