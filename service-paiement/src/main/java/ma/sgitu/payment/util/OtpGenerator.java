package ma.sgitu.payment.util;

import java.security.SecureRandom;

/**
 * Générateur de code OTP
 */
public class OtpGenerator {

    private static final SecureRandom random = new SecureRandom();
    private static final int OTP_LENGTH = 6;

    /**
     * Génère un code OTP de 6 chiffres
     * @return String OTP
     */
    public static String generate() {
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    /**
     * Génère un code OTP de longueur personnalisée
     * @param length Longueur du code
     * @return String OTP
     */
    public static String generate(int length) {
        int bound = (int) Math.pow(10, length);
        int otp = bound / 10 + random.nextInt(bound - bound / 10);
        return String.valueOf(otp);
    }
}