package ma.sgitu.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.sgitu.payment.dto.request.AddCardRequest;
import ma.sgitu.payment.dto.request.AddMobileMoneyRequest;
import ma.sgitu.payment.dto.response.PaymentAccountResponse;
import ma.sgitu.payment.entity.PaymentAccount;
import ma.sgitu.payment.entity.TestCard;
import ma.sgitu.payment.entity.TestMobileMoneyAccount;
import ma.sgitu.payment.enums.AccountStatus;
import ma.sgitu.payment.enums.PaymentMethod;
import ma.sgitu.payment.exception.BadRequestException;
import ma.sgitu.payment.repository.PaymentAccountRepository;
import ma.sgitu.payment.repository.TestCardRepository;
import ma.sgitu.payment.repository.TestMobileMoneyAccountRepository;
import ma.sgitu.payment.util.HashUtil;
import ma.sgitu.payment.util.MaskingUtil;
import ma.sgitu.payment.util.TokenGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentAccountService {

    private final PaymentAccountRepository paymentAccountRepository;
    private final TestCardRepository testCardRepository;
    private final TestMobileMoneyAccountRepository testMobileMoneyRepository;
    private final OtpService otpService;
    private final NotificationService notificationService;

    // ✅ AJOUT CARTE (CORRIGÉ)
    @Transactional
    public PaymentAccountResponse addCard(AddCardRequest request) {

        log.info("Ajout carte pour userId: {}", request.getUserId());

        // ✅ 1. Rechercher la carte via BCrypt verify()
        TestCard testCard = testCardRepository.findAll().stream()
                .filter(card -> HashUtil.verify(request.getCardNumber(), card.getCardNumberHash()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Carte non reconnue"));

        // ✅ 2. Vérifier CVV
        if (!HashUtil.verify(request.getCvv(), testCard.getCvvHash())) {
            throw new BadRequestException("CVV incorrect");
        }

        // ✅ 3. Vérifier statut
        if (testCard.getStatus() != AccountStatus.ACTIVE) {
            throw new BadRequestException("Carte bloquée ou expirée");
        }

        // ✅ 4. Générer token
        String paymentToken = TokenGenerator.generateCardToken();
        String maskedCard = MaskingUtil.maskCardNumber(request.getCardNumber());

        PaymentAccount account = PaymentAccount.builder()
                .userId(request.getUserId())
                .paymentMethod(PaymentMethod.CARD)
                .paymentToken(paymentToken)
                .maskedIdentifier(maskedCard)
                .provider(testCard.getProvider())
                .balance(testCard.getBalance())
                .status(AccountStatus.PENDING_VERIFICATION)
                .expiryMonth(request.getExpiryMonth())
                .expiryYear(request.getExpiryYear())
                .build();

        account = paymentAccountRepository.save(account);

        // ✅ 5. Générer OTP
        String otpCode = otpService.generateOtp(account);
        notificationService.sendOtpNotification(account, otpCode, request.getEmail());

        log.info("Carte ajoutée avec succès, OTP envoyé: {}", paymentToken);

        return toResponse(account);
    }

    // ✅ AJOUT MOBILE MONEY (CORRIGÉ)
    @Transactional
    public PaymentAccountResponse addMobileMoney(AddMobileMoneyRequest request) {

        log.info("Ajout Mobile Money pour userId: {}", request.getUserId());

        // ✅ Rechercher numéro via BCrypt verify()
        TestMobileMoneyAccount testAccount = testMobileMoneyRepository.findAll().stream()
                .filter(acc -> HashUtil.verify(request.getPhoneNumber(), acc.getPhoneHash()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Numéro Mobile Money non reconnu"));

        if (!testAccount.getProvider().equals(request.getProvider())) {
            throw new BadRequestException("Provider incorrect pour ce numéro");
        }

        if (testAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new BadRequestException("Compte Mobile Money bloqué");
        }

        String paymentToken = TokenGenerator.generateMobileMoneyToken();
        String maskedPhone = MaskingUtil.maskPhoneNumber(request.getPhoneNumber());

        PaymentAccount account = PaymentAccount.builder()
                .userId(request.getUserId())
                .paymentMethod(PaymentMethod.MOBILE_MONEY)
                .paymentToken(paymentToken)
                .maskedIdentifier(maskedPhone)
                .provider(request.getProvider())
                .balance(testAccount.getBalance())
                .status(AccountStatus.PENDING_VERIFICATION)
                .build();

        account = paymentAccountRepository.save(account);

        String otpCode = otpService.generateOtp(account);
        notificationService.sendOtpNotification(account, otpCode, request.getEmail());

        log.info("Mobile Money ajouté avec succès: {}", paymentToken);

        return toResponse(account);
    }

    public List<PaymentAccountResponse> getByUserId(Long userId) {
        return paymentAccountRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public PaymentAccountResponse getById(Long id) {
        PaymentAccount account = paymentAccountRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Moyen de paiement introuvable"));
        return toResponse(account);
    }

    @Transactional
    public void delete(Long id) {
        PaymentAccount account = paymentAccountRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Moyen de paiement introuvable"));
        paymentAccountRepository.delete(account);
        log.info("Moyen de paiement supprimé: {}", id);
    }

    private PaymentAccountResponse toResponse(PaymentAccount account) {
        return PaymentAccountResponse.builder()
                .id(account.getId())
                .userId(account.getUserId())
                .paymentMethod(account.getPaymentMethod().name())
                .paymentToken(account.getPaymentToken())
                .maskedIdentifier(account.getMaskedIdentifier())
                .provider(account.getProvider())
                .balance(account.getBalance())
                .status(account.getStatus().name())
                .expiryMonth(account.getExpiryMonth())
                .expiryYear(account.getExpiryYear())
                .createdAt(account.getCreatedAt())
                .build();
    }
}