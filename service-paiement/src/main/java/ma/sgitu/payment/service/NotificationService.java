package ma.sgitu.payment.service;

import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    public void sendOtpEmail(Long userId, String email, String otp, Long accountId) {
        // Temporaire — sera implémenté par Personne 4
        System.out.println(" OTP envoyé à : " + email + " | Code : " + otp);
    }

    public void sendPaymentSuccess(Long userId, String email, Long paymentId) {
        System.out.println(" Paiement SUCCESS - userId=" + userId);
    }

    public void sendPaymentFailed(Long userId, String email, Long paymentId) {
        System.out.println("Paiement FAILED - userId=" + userId);
    }

    public void sendPaymentCancelled(Long userId, String email, Long paymentId) {
        System.out.println(" Paiement CANCELLED - userId=" + userId);
    }
}