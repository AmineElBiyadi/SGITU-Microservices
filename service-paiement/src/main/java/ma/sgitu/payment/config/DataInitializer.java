package ma.sgitu.payment.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.sgitu.payment.entity.*;
import ma.sgitu.payment.enums.*;
import ma.sgitu.payment.repository.*;
import ma.sgitu.payment.util.HashUtil;
import ma.sgitu.payment.util.TokenGenerator;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final TestCardRepository testCardRepository;
    private final TestMobileMoneyAccountRepository testMobileMoneyRepository;
    private final PaymentAccountRepository paymentAccountRepository;
    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentOtpRepository paymentOtpRepository;

    @Bean
    @Transactional
    public CommandLineRunner initTestData() {
        return args -> {
            log.info("=== Initialisation des données de test complètes ===");

            if (testCardRepository.count() == 0) {
                initTestCards();
                initTestMobileMoneyAccounts();
                initPaymentAccounts();
                initPayments();
                initInvoices();
                log.info("=== ✅ Toutes les données de test ont été créées avec succès ===");
            } else {
                log.info("=== ℹ️ Données de test déjà présentes - skip initialisation ===");
            }
        };
    }

    private void initTestCards() {
        log.info("--- Création des cartes de test ---");
        
        createTestCard("4532015112830366", "123", "VISA", new BigDecimal("1000.00"), AccountStatus.ACTIVE);
        createTestCard("5425233430109903", "456", "MASTERCARD", new BigDecimal("500.00"), AccountStatus.ACTIVE);
        createTestCard("4916338506082832", "789", "VISA", new BigDecimal("50.00"), AccountStatus.ACTIVE);
        createTestCard("5105105105105100", "321", "MASTERCARD", new BigDecimal("0.00"), AccountStatus.ACTIVE);
        createTestCard("4111111111111111", "111", "VISA", new BigDecimal("2000.00"), AccountStatus.BLOCKED);
        createTestCard("5500000000000004", "999", "MASTERCARD", new BigDecimal("150.00"), AccountStatus.ACTIVE);
        createTestCard("4012888888881881", "555", "VISA", new BigDecimal("750.00"), AccountStatus.ACTIVE);
        
        log.info("✅ {} cartes de test créées", testCardRepository.count());
    }

    private void initTestMobileMoneyAccounts() {
        log.info("--- Création des comptes Mobile Money de test ---");
        
        createTestMobileMoneyAccount("0612345678", "INWI", new BigDecimal("500.00"), AccountStatus.ACTIVE);
        createTestMobileMoneyAccount("0623456789", "ORANGE", new BigDecimal("300.00"), AccountStatus.ACTIVE);
        createTestMobileMoneyAccount("0634567890", "IAM", new BigDecimal("1000.00"), AccountStatus.ACTIVE);
        createTestMobileMoneyAccount("0645678901", "INWI", new BigDecimal("0.00"), AccountStatus.ACTIVE);
        createTestMobileMoneyAccount("0656789012", "ORANGE", new BigDecimal("750.00"), AccountStatus.BLOCKED);
        createTestMobileMoneyAccount("0667890123", "IAM", new BigDecimal("250.00"), AccountStatus.ACTIVE);
        
        log.info("✅ {} comptes Mobile Money créés", testMobileMoneyRepository.count());
    }

    private void initPaymentAccounts() {
        log.info("--- Création des moyens de paiement enregistrés ---");
        
        createPaymentAccount(1L, PaymentMethod.CARD, "CARD-TOKEN-001", "****0366", "VISA", new BigDecimal("1000.00"), AccountStatus.ACTIVE, 12, 2027);
        createPaymentAccount(1L, PaymentMethod.MOBILE_MONEY, "MM-TOKEN-001", "0612****78", "INWI", new BigDecimal("500.00"), AccountStatus.ACTIVE, null, null);
        
        createPaymentAccount(2L, PaymentMethod.CARD, "CARD-TOKEN-002", "****9903", "MASTERCARD", new BigDecimal("500.00"), AccountStatus.ACTIVE, 12, 2027);
        createPaymentAccount(2L, PaymentMethod.MOBILE_MONEY, "MM-TOKEN-002", "0623****89", "ORANGE", new BigDecimal("300.00"), AccountStatus.ACTIVE, null, null);
        
        createPaymentAccount(3L, PaymentMethod.CARD, "CARD-TOKEN-003", "****2832", "VISA", new BigDecimal("50.00"), AccountStatus.ACTIVE, 12, 2027);
        createPaymentAccount(3L, PaymentMethod.MOBILE_MONEY, "MM-TOKEN-003", "0634****90", "IAM", new BigDecimal("1000.00"), AccountStatus.ACTIVE, null, null);
        
        createPaymentAccount(4L, PaymentMethod.CARD, "CARD-TOKEN-004", "****5100", "MASTERCARD", new BigDecimal("0.00"), AccountStatus.ACTIVE, 12, 2027);
        createPaymentAccount(5L, PaymentMethod.CARD, "CARD-TOKEN-005", "****1111", "VISA", new BigDecimal("2000.00"), AccountStatus.BLOCKED, 12, 2027);
        
        createPaymentAccount(6L, PaymentMethod.CARD, "CARD-TOKEN-006", "****0004", "MASTERCARD", new BigDecimal("150.00"), AccountStatus.ACTIVE, 6, 2028);
        createPaymentAccount(7L, PaymentMethod.MOBILE_MONEY, "MM-TOKEN-007", "0667****23", "IAM", new BigDecimal("250.00"), AccountStatus.ACTIVE, null, null);
        
        log.info("✅ {} moyens de paiement enregistrés créés", paymentAccountRepository.count());
    }

    private void initPayments() {
        log.info("--- Création des paiements de test ---");
        
        createPayment(1L, SourceType.TICKET, 101L, new BigDecimal("25.00"), PaymentMethod.CARD, "CARD-TOKEN-001", PaymentStatus.SUCCESS, null);
        createPayment(1L, SourceType.TICKET, 102L, new BigDecimal("30.00"), PaymentMethod.MOBILE_MONEY, "MM-TOKEN-001", PaymentStatus.SUCCESS, null);
        createPayment(1L, SourceType.SUBSCRIPTION, 201L, new BigDecimal("150.00"), PaymentMethod.CARD, "CARD-TOKEN-001", PaymentStatus.SUCCESS, null);
        
        createPayment(2L, SourceType.TICKET, 103L, new BigDecimal("20.00"), PaymentMethod.CARD, "CARD-TOKEN-002", PaymentStatus.SUCCESS, null);
        createPayment(2L, SourceType.SUBSCRIPTION, 202L, new BigDecimal("150.00"), PaymentMethod.MOBILE_MONEY, "MM-TOKEN-002", PaymentStatus.SUCCESS, null);
        
        createPayment(3L, SourceType.TICKET, 104L, new BigDecimal("15.00"), PaymentMethod.CARD, "CARD-TOKEN-003", PaymentStatus.FAILED, FailureReason.INSUFFICIENT_BALANCE);
        createPayment(3L, SourceType.SUBSCRIPTION, 203L, new BigDecimal("150.00"), PaymentMethod.MOBILE_MONEY, "MM-TOKEN-003", PaymentStatus.SUCCESS, null);
        
        createPayment(4L, SourceType.TICKET, 105L, new BigDecimal("40.00"), PaymentMethod.CARD, "CARD-TOKEN-004", PaymentStatus.FAILED, FailureReason.INSUFFICIENT_BALANCE);
        
        createPayment(5L, SourceType.TICKET, 106L, new BigDecimal("50.00"), PaymentMethod.CARD, "CARD-TOKEN-005", PaymentStatus.FAILED, FailureReason.ACCOUNT_BLOCKED);
        
        createPayment(1L, SourceType.TICKET, 107L, new BigDecimal("35.00"), PaymentMethod.CARD, "CARD-TOKEN-001", PaymentStatus.PENDING, null);
        createPayment(2L, SourceType.TICKET, 108L, new BigDecimal("25.00"), PaymentMethod.CARD, "CARD-TOKEN-002", PaymentStatus.CANCELLED, null);
        
        log.info("✅ {} paiements créés", paymentRepository.count());
    }

    private void initInvoices() {
        log.info("--- Création des factures pour paiements SUCCESS ---");
        
        paymentRepository.findAll().stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.SUCCESS)
                .forEach(payment -> {
                    if (!invoiceRepository.existsByPaymentId(payment.getId())) {
                        createInvoice(payment);
                    }
                });
        
        log.info("✅ {} factures générées", invoiceRepository.count());
    }

    private void createTestCard(String cardNumber, String cvv, String provider, BigDecimal balance, AccountStatus status) {
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

    private void createTestMobileMoneyAccount(String phone, String provider, BigDecimal balance, AccountStatus status) {
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

    private void createPaymentAccount(Long userId, PaymentMethod paymentMethod, String paymentToken, 
                                     String maskedIdentifier, String provider, BigDecimal balance, 
                                     AccountStatus status, Integer expiryMonth, Integer expiryYear) {
        try {
            PaymentAccount account = PaymentAccount.builder()
                    .userId(userId)
                    .paymentMethod(paymentMethod)
                    .paymentToken(paymentToken)
                    .maskedIdentifier(maskedIdentifier)
                    .provider(provider)
                    .balance(balance)
                    .status(status)
                    .expiryMonth(expiryMonth)
                    .expiryYear(expiryYear)
                    .build();

            paymentAccountRepository.save(account);
            log.debug("PaymentAccount créé : {} - {}", paymentToken, maskedIdentifier);
        } catch (Exception e) {
            log.error("Erreur création PaymentAccount {}: {}", paymentToken, e.getMessage());
        }
    }

    private void createPayment(Long userId, SourceType sourceType, Long sourceId, BigDecimal amount, 
                              PaymentMethod paymentMethod, String savedPaymentToken, 
                              PaymentStatus status, FailureReason failureReason) {
        try {
            Payment payment = Payment.builder()
                    .transactionToken(TokenGenerator.generateTransactionToken())
                    .userId(userId)
                    .sourceType(sourceType)
                    .sourceId(sourceId)
                    .amount(amount)
                    .paymentMethod(paymentMethod)
                    .savedPaymentToken(savedPaymentToken)
                    .status(status)
                    .failureReason(failureReason)
                    .build();

            paymentRepository.save(payment);
            log.debug("Payment créé : {} - {} - {}", payment.getTransactionToken(), status, amount);
        } catch (Exception e) {
            log.error("Erreur création Payment : {}", e.getMessage());
        }
    }

    private void createInvoice(Payment payment) {
        try {
            String invoiceNumber = "INV-PAY-" + payment.getId();

            Invoice invoice = Invoice.builder()
                    .invoiceNumber(invoiceNumber)
                    .payment(payment)
                    .userId(payment.getUserId())
                    .sourceType(payment.getSourceType())
                    .sourceId(payment.getSourceId())
                    .totalAmount(payment.getAmount())
                    .paymentMethod(payment.getPaymentMethod())
                    .build();

            invoiceRepository.save(invoice);
            log.debug("Invoice créée : {}", invoiceNumber);
        } catch (Exception e) {
            log.error("Erreur création Invoice pour payment {}: {}", payment.getId(), e.getMessage());
        }
    }
}