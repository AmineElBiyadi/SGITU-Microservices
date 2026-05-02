package ma.sgitu.payment.service;

import lombok.RequiredArgsConstructor;
import ma.sgitu.payment.dto.request.AddCardRequest;
import ma.sgitu.payment.dto.request.AddMobileMoneyRequest;
import ma.sgitu.payment.dto.request.VerifyOtpRequest;
import ma.sgitu.payment.dto.response.PaymentAccountResponse;
import ma.sgitu.payment.dto.response.TestCardResponse;
import ma.sgitu.payment.entity.PaymentAccount;
import ma.sgitu.payment.entity.TestCard;
import ma.sgitu.payment.entity.TestMobileMoneyAccount;
import ma.sgitu.payment.exception.BadRequestException;
import ma.sgitu.payment.exception.NotFoundException;
import ma.sgitu.payment.repository.PaymentAccountRepository;
import ma.sgitu.payment.repository.TestCardRepository;
import ma.sgitu.payment.repository.TestMobileMoneyAccountRepository;
import ma.sgitu.payment.util.HashUtil;
import ma.sgitu.payment.util.MaskingUtil;
import ma.sgitu.payment.util.TokenGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentAccountService {

    // ── Repositories ──────────────────────────────────────────────────────────
    private final PaymentAccountRepository accountRepository;
    private final TestCardRepository testCardRepository;
    private final TestMobileMoneyAccountRepository testMobileMoneyAccountRepository;

    // ── Services ──────────────────────────────────────────────────────────────
    private final OtpService otpService;
    private final NotificationService notificationService;   // implémenté par P4

    // ── Utils ─────────────────────────────────────────────────────────────────
    private final HashUtil hashUtil;
    private final MaskingUtil maskingUtil;
    private final TokenGenerator tokenGenerator;

    // ═════════════════════════════════════════════════════════════════════════
    // PARTIE CARD — développée par Personne 2
    // ═════════════════════════════════════════════════════════════════════════

    @Transactional
    public PaymentAccountResponse addCard(AddCardRequest request) {
        String cardHash = hashUtil.hash(request.getCardNumber());
        String cvvHash  = hashUtil.hash(request.getCvv());

        // 1. Carte existe dans test_cards ?
        TestCard testCard = testCardRepository.findByCardNumberHash(cardHash)
                .orElseThrow(() -> new BadRequestException(
                        "Carte introuvable dans notre système de test."));

        // 2. CVV correct ?
        if (!testCard.getCvvHash().equals(cvvHash)) {
            throw new BadRequestException("CVV incorrect.");
        }

        // 3. Statut ACTIVE ?
        if (!"ACTIVE".equals(testCard.getStatus())) {
            throw new BadRequestException("Carte " + testCard.getStatus().toLowerCase() + ".");
        }

        // 4. Date d'expiration valide ?
        LocalDateTime now = LocalDateTime.now();
        if (testCard.getExpiryYear() < now.getYear() ||
                (testCard.getExpiryYear() == now.getYear()
                        && testCard.getExpiryMonth() < now.getMonthValue())) {
            throw new BadRequestException("Carte expirée.");
        }

        // 5. Créer payment_account en PENDING_VERIFICATION
        PaymentAccount account = PaymentAccount.builder()
                .userId(request.getUserId())
                .paymentMethod("CARD")
                .paymentToken(tokenGenerator.generateCardToken())
                .maskedIdentifier(maskingUtil.maskCardNumber(request.getCardNumber()))
                .provider(testCard.getProvider())
                .balance(testCard.getBalance())
                .status("PENDING_VERIFICATION")
                .expiryMonth(testCard.getExpiryMonth())
                .expiryYear(testCard.getExpiryYear())
                .createdAt(now)
                .updatedAt(now)
                .build();

        account = accountRepository.save(account);

        // 6. Générer OTP + envoyer via G5
        String otp = otpService.generateAndSave(request.getUserId(), account);
        notificationService.sendOtpEmail(request.getUserId(), request.getEmail(), otp, account.getId());

        return toResponse(account);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PARTIE MOBILE_MONEY — développée par Personne 3
    // ═════════════════════════════════════════════════════════════════════════

    @Transactional
    public PaymentAccountResponse addMobileMoney(AddMobileMoneyRequest request) {
        // 1. Hacher le numéro de téléphone
        String hashedPhone = HashUtil.hashValue(request.getPhoneNumber());

        // 2. Vérifier existence dans test_mobile_money_accounts
        TestMobileMoneyAccount testAccount = testMobileMoneyAccountRepository
                .findByPhoneHash(hashedPhone)
                .orElseThrow(() -> new BadRequestException(
                        "Le numéro " + request.getPhoneNumber()
                                + " n'existe pas chez " + request.getProvider()));

        // 3. Provider correspond ?
        if (!testAccount.getProvider().equals(request.getProvider())) {
            throw new BadRequestException(
                    "L'opérateur choisi ne correspond pas au numéro de téléphone.");
        }

        // 4. Compte ACTIVE ?
        if (!"ACTIVE".equals(testAccount.getStatus())) {
            throw new BadRequestException(
                    "Ce compte Mobile Money est actuellement bloqué ou inactif.");
        }

        // 5. Créer payment_account en PENDING_VERIFICATION
        LocalDateTime now = LocalDateTime.now();
        PaymentAccount account = PaymentAccount.builder()
                .userId(request.getUserId())
                .paymentMethod("MOBILE_MONEY")
                .paymentToken(tokenGenerator.generateMobileMoneyToken())
                .maskedIdentifier(testAccount.getMaskedPhone())
                .provider(testAccount.getProvider())
                .balance(testAccount.getBalance())
                .status("PENDING_VERIFICATION")
                .createdAt(now)
                .updatedAt(now)
                .build();

        account = accountRepository.save(account);

        // 6. Générer OTP + envoyer via G5
        String otp = otpService.generateAndSave(request.getUserId(), account);
        notificationService.sendOtpEmail(request.getUserId(), request.getEmail(), otp, account.getId());

        return toResponse(account);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // VERIFY OTP — commun CARD + MOBILE_MONEY (P2 + P3 collaborent ici)
    // ═════════════════════════════════════════════════════════════════════════

    @Transactional
    public PaymentAccountResponse verifyOtp(Long paymentAccountId, VerifyOtpRequest request) {
        PaymentAccount account = accountRepository.findById(paymentAccountId)
                .orElseThrow(() -> new NotFoundException("Moyen de paiement introuvable."));

        // Vérifier appartenance
        if (!account.getUserId().equals(request.getUserId())) {
            throw new BadRequestException(
                    "Ce moyen de paiement n'appartient pas à cet utilisateur.");
        }

        // Vérifier statut
        if (!"PENDING_VERIFICATION".equals(account.getStatus())) {
            throw new BadRequestException(
                    "Ce moyen de paiement n'est pas en attente de vérification.");
        }

        // Vérifier OTP via OtpService (lève exception si invalide)
        otpService.verifyOtp(paymentAccountId, request.getOtp());

        // Activer
        account.setStatus("ACTIVE");
        account.setUpdatedAt(LocalDateTime.now());
        account = accountRepository.save(account);

        return toResponse(account);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // MÉTHODES COMMUNES
    // ═════════════════════════════════════════════════════════════════════════

    public List<PaymentAccountResponse> getByUserId(Long userId) {
        return accountRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public PaymentAccountResponse getById(Long paymentAccountId) {
        return toResponse(accountRepository.findById(paymentAccountId)
                .orElseThrow(() -> new NotFoundException("Moyen de paiement introuvable.")));
    }

    @Transactional
    public void delete(Long paymentAccountId) {
        PaymentAccount account = accountRepository.findById(paymentAccountId)
                .orElseThrow(() -> new NotFoundException("Moyen de paiement introuvable."));
        accountRepository.delete(account);
    }

    // ── TEST DATA ─────────────────────────────────────────────────────────────

    public List<TestCardResponse> getTestCards() {
        return testCardRepository.findAll()
                .stream()
                .map(this::toTestCardResponse)
                .collect(Collectors.toList());
    }

    // ═════════════════════════════════════════════════════════════════════════
    // MAPPERS INTERNES
    // ═════════════════════════════════════════════════════════════════════════

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
                //  Pas de cardNumberHash, pas de cvvHash → sécurité
                .build();
    }
}