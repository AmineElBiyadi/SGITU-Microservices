package ma.sgitu.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Application principale - Microservice G6 Paiement
 *
 * Fonctionnalités :
 * - Gestion des transactions de paiement
 * - Enregistrement des moyens de paiement (CARD, MOBILE_MONEY)
 * - Validation OTP via G5 Notifications
 * - Génération de factures
 * - Simulation de paiement (test_cards, test_mobile_money_accounts)
 *
 * Intégrations :
 * - G1 Billetterie : sourceType=TICKET
 * - G2 Abonnements : sourceType=SUBSCRIPTION
 * - G5 Notifications : OpenFeign (EMAIL réel)
 * - G10 Gateway : routing + JWT
 */
@SpringBootApplication
@EnableFeignClients // ✅ Active OpenFeign pour appeler G5
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}