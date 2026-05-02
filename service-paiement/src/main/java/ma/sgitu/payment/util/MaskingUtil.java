package ma.sgitu.payment.util;

import org.springframework.stereotype.Component;

@Component
public class MaskingUtil {

    // Masque un numéro de carte : ****1234
    public String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) return "****";
        return "****" + cardNumber.substring(cardNumber.length() - 4);
    }

    // Masque un numéro de téléphone : 0612****78
    public String maskPhoneNumber(String phone) {
        if (phone == null || phone.length() < 6) return "******";
        return phone.substring(0, 4) + "****" + phone.substring(phone.length() - 2);
    }
}