package ma.sgitu.payment.util;

public class MaskingUtil {
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 8) return "****";
        // Garde les 4 premiers chiffres, met 4 étoiles, et garde les 2 derniers
        return phone.substring(0, 4) + "****" + phone.substring(phone.length() - 2);
    }
}