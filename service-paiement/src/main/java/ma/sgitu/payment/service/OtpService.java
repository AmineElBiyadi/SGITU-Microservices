package ma.sgitu.payment.service;

import lombok.RequiredArgsConstructor;
import ma.sgitu.payment.entity.PaymentAccount;
import ma.sgitu.payment.entity.PaymentOtp;
import ma.sgitu.payment.exception.BadRequestException;
import ma.sgitu.payment.repository.PaymentOtpRepository;
import ma.sgitu.payment.util.HashUtil;
import ma.sgitu.payment.util.OtpGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final PaymentOtpRepository otpRepository;
    private final HashUtil hashUtil;
    private final OtpGenerator otpGenerator;

    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int MAX_ATTEMPTS = 5;

    @Transactional
    public String generateAndSave(Long userId, PaymentAccount account) {
        String otp = otpGenerator.generate();
        String otpHash = hashUtil.hash(otp);

        PaymentOtp paymentOtp = PaymentOtp.builder()
                .userId(userId)
                .paymentAccount(account)
                .otpHash(otpHash)
                .status("PENDING")
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .attempts(0)
                .createdAt(LocalDateTime.now())
                .build();

        otpRepository.save(paymentOtp);
        return otp; // retourné en clair pour envoi via G5
    }

    @Transactional
    public void verifyOtp(Long paymentAccountId, String rawOtp) {
        Optional<PaymentOtp> optOtp = otpRepository
                .findTopByPaymentAccountIdAndStatusOrderByCreatedAtDesc(paymentAccountId, "PENDING");

        if (optOtp.isEmpty()) {
            throw new BadRequestException("Aucun OTP actif trouvé pour ce moyen de paiement.");
        }

        PaymentOtp otp = optOtp.get();

        // Vérifier expiration
        if (LocalDateTime.now().isAfter(otp.getExpiresAt())) {
            otp.setStatus("EXPIRED");
            otpRepository.save(otp);
            throw new BadRequestException("OTP expiré.");
        }

        // Vérifier max tentatives
        if (otp.getAttempts() >= MAX_ATTEMPTS) {
            otp.setStatus("FAILED");
            otpRepository.save(otp);
            throw new BadRequestException("Nombre maximum de tentatives atteint.");
        }

        otp.setAttempts(otp.getAttempts() + 1);

        // Vérifier le code
        if (!hashUtil.matches(rawOtp, otp.getOtpHash())) {
            otpRepository.save(otp);
            throw new BadRequestException("Code OTP incorrect. Tentative " + otp.getAttempts() + "/" + MAX_ATTEMPTS);
        }

        otp.setStatus("VERIFIED");
        otp.setVerifiedAt(LocalDateTime.now());
        otpRepository.save(otp);
    }
}