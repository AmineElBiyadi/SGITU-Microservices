package ma.sgitu.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.sgitu.payment.entity.PaymentAccount;
import ma.sgitu.payment.entity.PaymentOtp;
import ma.sgitu.payment.enums.OtpStatus;
import ma.sgitu.payment.repository.PaymentOtpRepository;
import ma.sgitu.payment.util.HashUtil;
import ma.sgitu.payment.util.OtpGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service de gestion OTP
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final PaymentOtpRepository paymentOtpRepository;

    /**
     * Génère un OTP pour un PaymentAccount
     */
    @Transactional
    public String generateOtp(PaymentAccount account) {

        String otpCode = OtpGenerator.generate();

        PaymentOtp otp = PaymentOtp.builder()
                .userId(account.getUserId())
                .paymentAccountId(account.getId())
                .otpHash(HashUtil.hash(otpCode))
                .status(OtpStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .attempts(0)
                .build();

        paymentOtpRepository.save(otp);

        log.info("OTP généré pour account {} : {}", account.getId(), otpCode);

        return otpCode;
    }
}