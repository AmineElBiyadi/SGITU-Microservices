package ma.sgitu.payment.service;

import lombok.RequiredArgsConstructor;
import ma.sgitu.payment.dto.request.AddMobileMoneyRequest;
import ma.sgitu.payment.entity.PaymentAccount;
import ma.sgitu.payment.entity.PaymentOtp;
import ma.sgitu.payment.entity.TestMobileMoneyAccount;
import ma.sgitu.payment.enums.AccountStatus;
import ma.sgitu.payment.enums.PaymentMethod;
import ma.sgitu.payment.exception.BadRequestException;
import ma.sgitu.payment.repository.PaymentAccountRepository;
import ma.sgitu.payment.repository.PaymentOtpRepository;
import ma.sgitu.payment.repository.TestMobileMoneyAccountRepository;
import ma.sgitu.payment.util.HashUtil;
import ma.sgitu.payment.util.MaskingUtil; // Si tu as cet utilitaire
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentAccountService {

    private final PaymentAccountRepository paymentAccountRepository;
    private final TestMobileMoneyAccountRepository testMobileMoneyAccountRepository;
    private final PaymentOtpRepository paymentOtpRepository;

    public String registerMobileMoney(AddMobileMoneyRequest request) {
        // 1. On hache le numéro pour chercher dans la simulation

        String hashedPhone = HashUtil.hashValue(request.getPhoneNumber());

        // 2. On vérifie si ce numéro "existe" chez l'opérateur simulé
        TestMobileMoneyAccount testAccount = testMobileMoneyAccountRepository
                .findByPhoneHash(hashedPhone)
                .orElseThrow(() -> new ma.sgitu.payment.exception.BadRequestException(
                        "Le numéro " + request.getPhoneNumber() + " n'existe pas chez " + request.getProvider()));
        if (!testAccount.getProvider().equals(request.getProvider())) {
            throw new BadRequestException("L'opérateur choisi ne correspond pas au numéro de téléphone.");
        }
        if (testAccount.getStatus() != ma.sgitu.payment.enums.AccountStatus.ACTIVE) {
            throw new BadRequestException("Ce compte Mobile Money est actuellement bloqué ou inactif chez l'opérateur.");
        }

        // 3. On crée le moyen de paiement pour l'utilisateur

        PaymentAccount account = PaymentAccount.builder()
                .userId(request.getUserId())
                .paymentMethod(PaymentMethod.MOBILE_MONEY)
                .provider(request.getProvider())
                .paymentToken("MM-TOKEN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .maskedIdentifier(testAccount.getMaskedPhone())
                .balance(testAccount.getBalance()) // On synchronise le solde simulé
                .status(AccountStatus.PENDING_VERIFICATION) // En attente de l'OTP
                .build();

        account = paymentAccountRepository.save(account);


        String rawOtp = String.valueOf((int)(Math.random() * 900000) + 100000);
        String hashedOtp = HashUtil.hashValue(rawOtp);

        PaymentOtp otpEntry = PaymentOtp.builder()
                .userId(request.getUserId())
                .paymentAccountId(account.getId())
                .otpHash(hashedOtp)
                .status("PENDING")
                .attempts(0)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();
        paymentOtpRepository.save(otpEntry);

        System.out.println("DEMANDE ENVOI G5 NOTIFICATIONS");
        System.out.println("DESTINATAIRE : " + request.getEmail());
        System.out.println("MESSAGE : Votre code de validation est " + rawOtp);

        return account.getPaymentToken();
    }
}