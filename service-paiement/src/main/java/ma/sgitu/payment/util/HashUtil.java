package ma.sgitu.payment.util;

import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class HashUtil {

    // Méthode instance (injection Spring — ton style P2)
    public String hash(String input) {
        return hashValue(input);
    }

    public boolean matches(String raw, String hashed) {
        return hashValue(raw).equals(hashed);
    }

    // Méthode statique (style P3 — pour compatibilité)
    public static String hashValue(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(encoded);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Erreur de hachage SHA-256", e);
        }
    }
}