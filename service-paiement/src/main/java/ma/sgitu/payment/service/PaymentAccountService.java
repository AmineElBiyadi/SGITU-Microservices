package ma.sgitu.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.sgitu.payment.dto.request.*;
import ma.sgitu.payment.dto.response.*;
import ma.sgitu.payment.entity.*;
import ma.sgitu.payment.enums.*;
import ma.sgitu.payment.exception.BadRequestException;
import ma.sgitu.payment.repository.*;
import ma.sgitu.payment.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentAccountService {

    private final PaymentAccountRepository paymentAccountRepository;
    private final TestCardRepository testCardRepository;
    private final TestMobileMoneyAccountRepository testMobileMoneyRepository;
    private final PaymentOtpRepository paymentOtpRepository;
    private final OtpService otpService;
    private final NotificationService notificationService;

    // ✅ ADD CARD
    @Transactional
    public PaymentAccountResponse addCard(AddCardRequest request) {

        TestCard testCard = testCardRepository.findAll().stream()
                .filter(card -> HashUtil.verify(request.getCardNumber(), card.getCardNumberHash()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Carte non reconnue"));

        if (!HashUtil.verify(request.getCvv(), testCard.getCvvHash())) {
            throw new BadRequestException("CVV incorrect");
        }

        if (testCard.getStatus() != AccountStatus.ACTIVE) {
            throw new BadRequestException("Carte bloquée ou expirée");
        }

        String token = TokenGenerator.generateCardToken();
        String masked = MaskingUtil.maskCardNumber(request.getCardNumber());

        PaymentAccount account = PaymentAccount.builder()
                .userId(request.getUserId())
                .paymentMethod(PaymentMethod.CARD)
                .paymentToken(token)
                .maskedIdentifier(masked)
                .provider(testCard.getProvider())
                .balance(testCard.getBalance())
                .status(AccountStatus.PENDING_VERIFICATION)
                .expiryMonth(request.getExpiryMonth())
                .expiryYear(request.getExpiryYear())
                .build();

        account = paymentAccountRepository.save(account);

        String otp = otpService.generateOtp(account);
        notificationService.sendOtpNotification(account, otp, request.getEmail());

        return toResponse(account);
    }

    // ✅ ADD MOBILE MONEY
    @Transactional
    public PaymentAccountResponse addMobileMoney(AddMobileMoneyRequest request) {

        TestMobileMoneyAccount testAccount = testMobileMoneyRepository.findAll().stream()
                .filter(acc -> HashUtil.verify(request.getPhoneNumber(), acc.getPhoneHash()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Numéro Mobile Money non reconnu"));

        if (!testAccount.getProvider().equals(request.getProvider())) {
            throw new BadRequestException("Provider incorrect");
        }

        if (testAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new BadRequestException("Compte Mobile Money bloqué");
        }

        String token = TokenGenerator.generateMobileMoneyToken();
        String masked = MaskingUtil.maskPhoneNumber(request.getPhoneNumber());

        PaymentAccount account = PaymentAccount.builder()
                .userId(request.getUserId())
                .paymentMethod(PaymentMethod.MOBILE_MONEY)
                .paymentToken(token)
                .maskedIdentifier(masked)
                .provider(request.getProvider())
                .balance(testAccount.getBalance())
                .status(AccountStatus.PENDING_VERIFICATION)
                .build();

        account = paymentAccountRepository.save(account);

        String otp = otpService.generateOtp(account);
        notificationService.sendOtpNotification(account, otp, request.getEmail());

        return toResponse(account);
    }

    // ✅ VERIFY OTP (SANS LAMBDA BUG)
    @Transactional
    public PaymentAccountResponse verifyOtp(VerifyOtpRequest request) {

        PaymentAccount account = paymentAccountRepository.findById(request.getPaymentAccountId())
                .orElseThrow(() -> new BadRequestException("Compte introuvable"));

        List<PaymentOtp> otps =
                paymentOtpRepository.findByPaymentAccountIdAndStatus(account.getId(), OtpStatus.PENDING);

        if (otps.isEmpty()) {
            throw new BadRequestException("Aucun OTP en attente");
        }

        PaymentOtp otp = otps.get(otps.size() - 1);

        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            otp.setStatus(OtpStatus.EXPIRED);
            paymentOtpRepository.save(otp);
            throw new BadRequestException("OTP expiré");
        }

        if (!HashUtil.verify(request.getOtpCode(), otp.getOtpHash())) {
            otp.setAttempts(otp.getAttempts() + 1);
            paymentOtpRepository.save(otp);
            throw new BadRequestException("OTP incorrect");
        }

        otp.setStatus(OtpStatus.VERIFIED);
        otp.setVerifiedAt(LocalDateTime.now());
        paymentOtpRepository.save(otp);

        account.setStatus(AccountStatus.ACTIVE);
        paymentAccountRepository.save(account);

        return toResponse(account);
    }

    public List<PaymentAccountResponse> getByUserId(Long userId) {
        return paymentAccountRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public PaymentAccountResponse getById(Long id) {
        PaymentAccount account = paymentAccountRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Introuvable"));
        return toResponse(account);
    }

    @Transactional
    public void delete(Long id) {
        PaymentAccount account = paymentAccountRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Introuvable"));
        paymentAccountRepository.delete(account);
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