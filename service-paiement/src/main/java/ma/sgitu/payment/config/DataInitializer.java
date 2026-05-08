package ma.sgitu.payment.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.sgitu.payment.entity.TestCard;
import ma.sgitu.payment.entity.TestMobileMoneyAccount;
import ma.sgitu.payment.enums.AccountStatus;
import ma.sgitu.payment.repository.TestCardRepository;
import ma.sgitu.payment.repository.TestMobileMoneyAccountRepository;
import ma.sgitu.payment.util.HashUtil;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Initialise les données de test au démarrage
 * Cartes et comptes Mobile Money fictifs
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final TestCardRepository testCardRepository;
    private final TestMobileMoneyAccountRepository testMobileMoneyRepository;

    @Bean
    @Transactional
    public CommandLineRunner initTestData() {
        return args -> {
            log.info("=== Initialisation des données de test ===");

            // ── Cartes fictives ──────────────────────────────
            if (testCardRepository.count() == 0) {
                createTestCard("4532015112830366", "123", "VISA", new BigDecimal("1000.00"), AccountStatus.ACTIVE);
                createTestCard("5425233430109903", "456", "MASTERCARD", new BigDecimal("500.00"), AccountStatus.ACTIVE);
                createTestCard("4916338506082832", "789", "VISA", new BigDecimal("50.00"), AccountStatus.ACTIVE);
                createTestCard("5105105105105100", "321", "MASTERCARD", new BigDecimal("0.00"), AccountStatus.ACTIVE);
                createTestCard("4111111111111111", "111", "VISA", new BigDecimal("2000.00"), AccountStatus.BLOCKED);

                log.info("✅ {} cartes de test créées", testCardRepository.count());
            } else {
                log.info("ℹ️ Cartes de test déjà présentes");
            }

            // ── Comptes Mobile Money fictifs ─────────────────
            if (testMobileMoneyRepository.count() == 0) {
                createTestMobileMoneyAccount("0612345678", "INWI", new BigDecimal("500.00"), AccountStatus.ACTIVE);
                createTestMobileMoneyAccount("0623456789", "ORANGE", new BigDecimal("300.00"), AccountStatus.ACTIVE);
                createTestMobileMoneyAccount("0634567890", "IAM", new BigDecimal("1000.00"), AccountStatus.ACTIVE);
                createTestMobileMoneyAccount("0645678901", "INWI", new BigDecimal("0.00"), AccountStatus.ACTIVE);
                createTestMobileMoneyAccount("0656789012", "ORANGE", new BigDecimal("750.00"), AccountStatus.BLOCKED);

                log.info("✅ {} comptes Mobile Money de test créés", testMobileMoneyRepository.count());
            } else {
                log.info("ℹ️ Comptes Mobile Money de test déjà présents");
            }

            log.info("=== Initialisation terminée ===");
        };
    }

    private void createTestCard(String cardNumber, String cvv, String provider,
                                BigDecimal balance, AccountStatus status) {
        try {
            String last4 = cardNumber.substring(cardNumber.length() - 4);

            TestCard card = TestCard.builder()
                    .cardNumberHash(HashUtil.hash(cardNumber))
                    .cvvHash(HashUtil.hash(cvv))
                    .last4(last4)
                    .cardHolderName("TEST USER " + last4)
                    .expiryMonth(12)
                    .expiryYear(2027)
                    .provider(provider)
                    .balance(balance)
                    .status(status)
                    .build();

            testCardRepository.save(card);
            log.debug("Carte créée : {} - {} DH", last4, balance);
        } catch (Exception e) {
            log.error("Erreur création carte {}: {}", cardNumber, e.getMessage());
        }
    }

    private void createTestMobileMoneyAccount(String phone, String provider,
                                              BigDecimal balance, AccountStatus status) {
        try {
            String maskedPhone = phone.substring(0, 4) + "****" + phone.substring(phone.length() - 2);

            TestMobileMoneyAccount account = TestMobileMoneyAccount.builder()
                    .phoneHash(HashUtil.hash(phone))
                    .maskedPhone(maskedPhone)
                    .provider(provider)
                    .balance(balance)
                    .status(status)
                    .build();

            testMobileMoneyRepository.save(account);
            log.debug("Mobile Money créé : {} ({}) - {} DH", maskedPhone, provider, balance);
        } catch (Exception e) {
            log.error("Erreur création Mobile Money {}: {}", phone, e.getMessage());
        }
    }
}