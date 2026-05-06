package ma.sgitu.payment.service;

import lombok.RequiredArgsConstructor;
import ma.sgitu.payment.dto.request.AddCardRequest;
import ma.sgitu.payment.dto.request.AddMobileMoneyRequest;
import ma.sgitu.payment.dto.request.VerifyOtpRequest;
import ma.sgitu.payment.dto.response.PaymentAccountResponse;
import ma.sgitu.payment.dto.response.TestCardResponse;
import ma.sgitu.payment.entity.PaymentAccount;
import ma.sgitu.payment.entity.PaymentOtp;
import ma.sgitu.payment.entity.TestCard;
import ma.sgitu.payment.entity.TestMobileMoneyAccount;
import ma.sgitu.payment.enums.AccountStatus;
import ma.sgitu.payment.enums.PaymentMethod;
import ma.sgitu.payment.exception.BadRequestException;
import ma.sgitu.payment.exception.NotFoundException;
import ma.sgitu.payment.repository.*;
import ma.sgitu.payment.util.HashUtil;
import ma.sgitu.payment.util.MaskingUtil;
import ma.sgitu.payment.util.TokenGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentAccountService {

    // Repositories unifiés
    private final PaymentAccountRepository paymentAccountRepository;
    private final TestCardRepository testCardRepository;
    private final TestMobileMoneyAccountRepository testMobileMoneyAccountRepository;
    private final PaymentOtpRepository paymentOtpRepository;

    // Services
    private final OtpService otpService;
    private final NotificationService notificationService;

    // Utils
    private final MaskingUtil maskingUtil;
    private final TokenGenerator tokenGenerator;

    // =========================================================================
    // PARTIE CARD (Personne 2)
    // =========================================================================
    @Transactional
    public PaymentAccountResponse addCard(AddCardRequest request) {
        // Utilisation du Hash sécurisé
        String cardHash = HashUtil.hashValue(request.getCardNumber());
        String cvvHash  = HashUtil.hashValue(request.getCvv());

        TestCard testCard = testCardRepository.findByCardNumberHash(cardHash)
                .orElseThrow(() -> new BadRequestException("Carte introuvable dans le système de test."));

        if (!testCard.getCvvHash().equals(cvvHash)) {
            throw new BadRequestException("CVV incorrect.");
        }

        // Correction : Comparaison de String
        if (!AccountStatus.ACTIVE.name().equals(testCard.getStatus())) {
            throw new BadRequestException("Carte non active.");
        }

        PaymentAccount account = PaymentAccount.builder()
                .userId(request.getUserId())
                .paymentMethod(PaymentMethod.CARD.name())
                .paymentToken(tokenGenerator.generateCardToken())
                .maskedIdentifier(maskingUtil.maskCardNumber(request.getCardNumber()))
                .provider(testCard.getProvider())
                .balance(testCard.getBalance())
                .status(AccountStatus.PENDING_VERIFICATION.name())
                .expiryMonth(testCard.getExpiryMonth())
                .expiryYear(testCard.getExpiryYear())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        account = paymentAccountRepository.save(account);

        // Génération OTP (Logique P2)
        String otp = otpService.generateAndSave(request.getUserId(), account);
        notificationService.sendOtpEmail(request.getUserId(), request.getEmail(), otp, account.getId());

        return toResponse(account);
    }

    // =========================================================================
    // PARTIE MOBILE_MONEY (Personne 3 - TON TRAVAIL)
    // =========================================================================
    @Transactional
    public PaymentAccountResponse addMobileMoney(AddMobileMoneyRequest request) {
        String hashedPhone = HashUtil.hashValue(request.getPhoneNumber().trim());
        System.out.println("--- DEBUG PERSONNE 3 ---");
        System.out.println("NUMÉRO REÇU : [" + request.getPhoneNumber() + "]");
        System.out.println("HASH CALCULÉ : " + hashedPhone);
        System.out.println("-------------------------");
        TestMobileMoneyAccount testAccount = testMobileMoneyAccountRepository
                .findByPhoneHash(hashedPhone)

                .orElseThrow(() -> new BadRequestException("Le numéro " + request.getPhoneNumber() + " n'existe pas."));

        if (!AccountStatus.ACTIVE.name().equals(testAccount.getStatus().name())) {
            throw new BadRequestException("Compte Mobile Money bloqué chez l'opérateur.");
        }

        if (!testAccount.getProvider().equals(request.getProvider())) {
            throw new BadRequestException("L'opérateur ne correspond pas au numéro.");
        }

        PaymentAccount account = PaymentAccount.builder()
                .userId(request.getUserId())
                .paymentMethod(PaymentMethod.MOBILE_MONEY.name())
                .paymentToken("MM-TOKEN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .maskedIdentifier(testAccount.getMaskedPhone())
                .provider(testAccount.getProvider())
                .balance(testAccount.getBalance())
                .status(AccountStatus.PENDING_VERIFICATION.name())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        account = paymentAccountRepository.save(account);

        // Sauvegarde OTP (Ton flux sécurisé)
        String rawOtp = String.valueOf((int)(Math.random() * 900000) + 100000);
        PaymentOtp otpEntry = PaymentOtp.builder()
                .userId(request.getUserId())
                .paymentAccount(account)
                .otpHash(HashUtil.hashValue(rawOtp))
                .status("PENDING")
                .attempts(0)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();
        paymentOtpRepository.save(otpEntry);

        System.out.println("DEBUG OTP : " + rawOtp);
        return toResponse(account);
    }

    // =========================================================================
    // VERIFY OTP (Collab P2 + P3)
    // =========================================================================
    @Transactional
    public PaymentAccountResponse verifyOtp(Long paymentAccountId, VerifyOtpRequest request) {
        PaymentAccount account = paymentAccountRepository.findById(paymentAccountId)
                .orElseThrow(() -> new NotFoundException("Compte introuvable."));

        otpService.verifyOtp(paymentAccountId, request.getOtp());

        account.setStatus(AccountStatus.ACTIVE.name());
        account.setUpdatedAt(LocalDateTime.now());
        return toResponse(paymentAccountRepository.save(account));
    }

    // =========================================================================
    // MÉTHODES COMMUNES ET DONNÉES TEST
    // =========================================================================
    public List<PaymentAccountResponse> getByUserId(Long userId) {
        return paymentAccountRepository.findByUserId(userId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    public PaymentAccountResponse getById(Long paymentAccountId) {
        return toResponse(paymentAccountRepository.findById(paymentAccountId)
                .orElseThrow(() -> new NotFoundException("Compte introuvable.")));
    }

    @Transactional
    public void delete(Long paymentAccountId) {
        paymentAccountRepository.deleteById(paymentAccountId);
    }

    public List<TestMobileMoneyAccount> getAllTestMobileAccounts() {
        return testMobileMoneyAccountRepository.findAll();
    }

    public List<TestCardResponse> getTestCards() {
        return testCardRepository.findAll().stream().map(this::toTestCardResponse).collect(Collectors.toList());
    }

    // =========================================================================
    // MAPPERS
    // =========================================================================
    private PaymentAccountResponse toResponse(PaymentAccount a) {
        return PaymentAccountResponse.builder()
                .id(a.getId())
                .userId(a.getUserId())
                .paymentMethod(a.getPaymentMethod())
                .paymentToken(a.getPaymentToken())
                .maskedIdentifier(a.getMaskedIdentifier())
                .provider(a.getProvider())
                .balance(a.getBalance())
                .status(a.getStatus())
                .expiryMonth(a.getExpiryMonth())
                .expiryYear(a.getExpiryYear())
                .createdAt(a.getCreatedAt())
                .build();
    }

    private TestCardResponse toTestCardResponse(TestCard c) {
        return TestCardResponse.builder()
                .id(c.getId())
                .last4(c.getLast4())
                .cardHolderName(c.getCardHolderName())
                .expiryMonth(c.getExpiryMonth())
                .expiryYear(c.getExpiryYear())
                .provider(c.getProvider())
                .balance(c.getBalance())
                .status(c.getStatus())
                .build();
    }
}