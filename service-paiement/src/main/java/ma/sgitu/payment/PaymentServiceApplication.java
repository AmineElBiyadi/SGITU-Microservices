package ma.sgitu.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application principale - Microservice G6 Paiement
 *
 * Fonctionnalités :
 * - Gestion des transactions de paiement
 * - Enregistrement des moyens de paiement (CARD, MOBILE_MONEY)
 * - Validation OTP via G5 Notifications
 * - Génération de factures
 * - Gestion des remboursements (TICKET + SUBSCRIPTION)
 * - Simulation de paiement (test_cards, test_mobile_money_accounts)
 *
 * Intégrations :
 * - G1 Billetterie : sourceType=TICKET + remboursement
 * - G2 Abonnements : sourceType=SUBSCRIPTION + callbacks
 * - G5 Notifications : Kafka (payment.notification topic)
 * - G10 Gateway : routing + JWT
 *
 * Sécurité :
 * - TLS/HTTPS (PKCS12 keystore)
 * - JWT Authentication
 * - BCrypt OTP hashing
 */
@SpringBootApplication
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}