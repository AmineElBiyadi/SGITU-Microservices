package ma.sgitu.payment.util;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TokenGenerator {

    public String generateCardToken() {
        return "CARD-TOKEN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public String generateMobileMoneyToken() {
        return "MM-TOKEN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public String generatePaymentToken() {
        return "PAY-TOKEN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}