package ma.sgitu.payment.service;

import lombok.RequiredArgsConstructor;
import ma.sgitu.payment.dto.request.PaymentRequest;
import ma.sgitu.payment.dto.response.PaymentDetailsResponse;
import ma.sgitu.payment.dto.response.PaymentResponse;
import ma.sgitu.payment.entity.Payment;
import ma.sgitu.payment.enums.PaymentStatus;
import ma.sgitu.payment.repository.PaymentAccountRepository;
import ma.sgitu.payment.repository.PaymentRepository;
import ma.sgitu.payment.repository.TestMobileMoneyAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentAccountRepository paymentAccountRepository;
    private final TestMobileMoneyAccountRepository testMobileMoneyAccountRepository;
    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {

        // Créer transaction PENDING
        Payment payment = Payment.builder()
                .userId(request.getUserId())
                .sourceType(request.getSourceType())
                .sourceId(request.getSourceId())
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .savedPaymentToken(request.getSavedPaymentToken())
                .status(PaymentStatus.PENDING)
                .transactionToken("PAY-" + System.currentTimeMillis())
                .build();

        if (request.getPaymentMethod() == ma.sgitu.payment.enums.PaymentMethod.MOBILE_MONEY) {
            handleMobileMoneyPayment(payment);
        }

        payment = paymentRepository.save(payment);
        String msg = payment.getStatus() == PaymentStatus.SUCCESS ? "Paiement réussi" : "Paiement en attente ou échoué";

        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .transactionToken(payment.getTransactionToken())
                .status(payment.getStatus().name())
                .failureReason(payment.getFailureReason() != null ? payment.getFailureReason().name() : null)
                .message(payment.getStatus() == PaymentStatus.SUCCESS ? "Paiement réussi" : "Le paiement a échoué")
                .build();
    }

    public PaymentDetailsResponse getPaymentById(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Paiement introuvable: " + paymentId));
        return toDetailsResponse(payment);
    }

    public List<PaymentDetailsResponse> getPaymentsByUserId(Long userId) {
        return paymentRepository.findByUserId(userId)
                .stream()
                .map(this::toDetailsResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public PaymentDetailsResponse cancelPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Paiement introuvable: " + paymentId));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new RuntimeException("Seul un paiement PENDING peut être annulé");
        }

        payment.setStatus(PaymentStatus.CANCELLED);
        paymentRepository.save(payment);
        return toDetailsResponse(payment);
    }

    private PaymentDetailsResponse toDetailsResponse(Payment payment) {
        return PaymentDetailsResponse.builder()
                .id(payment.getId())
                .transactionToken(payment.getTransactionToken())
                .userId(payment.getUserId())
                .sourceType(payment.getSourceType().name())
                .sourceId(payment.getSourceId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod().name())
                .savedPaymentToken(payment.getSavedPaymentToken())
                .status(payment.getStatus().name())
                .failureReason(payment.getFailureReason() != null ? payment.getFailureReason().name() : null)
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }

    private void handleMobileMoneyPayment(Payment payment) {
        // 1. Chercher le compte
        ma.sgitu.payment.entity.PaymentAccount account = paymentAccountRepository
                .findByPaymentToken(payment.getSavedPaymentToken())
                .orElse(null);

        if (account == null) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(ma.sgitu.payment.enums.FailureReason.INVALID_TOKEN);
            return;
        }

        // 2. Vérifier Statut — ✅ String comparé avec String
        if (!"ACTIVE".equals(account.getStatus())) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(ma.sgitu.payment.enums.FailureReason.ACCOUNT_NOT_ACTIVE);
        } else if (account.getBalance().compareTo(payment.getAmount()) < 0) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(ma.sgitu.payment.enums.FailureReason.INSUFFICIENT_BALANCE);
        } else {
            // 3. Débit et Succès
            account.setBalance(account.getBalance().subtract(payment.getAmount()));
            paymentAccountRepository.save(account);
            testMobileMoneyAccountRepository.findByMaskedPhone(account.getMaskedIdentifier())
                    .ifPresent(testAccount -> {
                        testAccount.setBalance(testAccount.getBalance().subtract(payment.getAmount()));
                        testMobileMoneyAccountRepository.save(testAccount);
                    });
            payment.setStatus(PaymentStatus.SUCCESS);
        }
    }
    }