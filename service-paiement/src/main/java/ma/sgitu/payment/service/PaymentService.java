package ma.sgitu.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.sgitu.payment.dto.request.PaymentRequest;
import ma.sgitu.payment.dto.response.PaymentDetailsResponse;
import ma.sgitu.payment.dto.response.PaymentResponse;
import ma.sgitu.payment.entity.Payment;
import ma.sgitu.payment.entity.PaymentAccount;
import ma.sgitu.payment.enums.AccountStatus;
import ma.sgitu.payment.enums.FailureReason;
import ma.sgitu.payment.enums.PaymentStatus;
import ma.sgitu.payment.exception.BadRequestException;
import ma.sgitu.payment.mapper.PaymentMapper;
import ma.sgitu.payment.repository.PaymentAccountRepository;
import ma.sgitu.payment.repository.PaymentRepository;
import ma.sgitu.payment.util.TokenGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentAccountRepository paymentAccountRepository;
    private final PaymentMapper paymentMapper;
    private final InvoiceService invoiceService;
    private final NotificationService notificationService;
    private final SubscriptionCallbackService subscriptionCallbackService;

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {

        log.info("Traitement paiement userId={}, sourceType={}, sourceId={}, amount={}",
                request.getUserId(), request.getSourceType(), request.getSourceId(), request.getAmount());

        Payment payment = Payment.builder()
                .userId(request.getUserId())
                .sourceType(request.getSourceType())
                .sourceId(request.getSourceId())
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .savedPaymentToken(request.getSavedPaymentToken())
                .status(PaymentStatus.PENDING)
                .transactionToken(TokenGenerator.generateTransactionToken())
                .build();

        payment = paymentRepository.save(payment);

        PaymentAccount account = paymentAccountRepository
                .findByPaymentToken(request.getSavedPaymentToken())
                .orElse(null);

        if (account == null) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(FailureReason.INVALID_TOKEN);
            paymentRepository.save(payment);
            notificationService.sendPaymentFailedNotification(payment, request.getEmail());
            subscriptionCallbackService.sendPaymentConfirmation(payment);
            return buildFailedResponse(payment);
        }

        if (!account.getUserId().equals(request.getUserId())) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(FailureReason.UNAUTHORIZED_TOKEN);
            paymentRepository.save(payment);
            notificationService.sendPaymentFailedNotification(payment, request.getEmail());
            subscriptionCallbackService.sendPaymentConfirmation(payment);
            return buildFailedResponse(payment);
        }

        if (account.getStatus() != AccountStatus.ACTIVE) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(FailureReason.ACCOUNT_NOT_ACTIVE);
            paymentRepository.save(payment);
            notificationService.sendPaymentFailedNotification(payment, request.getEmail());
            subscriptionCallbackService.sendPaymentConfirmation(payment);
            return buildFailedResponse(payment);
        }

        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(FailureReason.INSUFFICIENT_BALANCE);
            paymentRepository.save(payment);
            notificationService.sendPaymentFailedNotification(payment, request.getEmail());
            subscriptionCallbackService.sendPaymentConfirmation(payment);
            return buildFailedResponse(payment);
        }

        account.setBalance(account.getBalance().subtract(request.getAmount()));
        paymentAccountRepository.save(account);

        payment.setStatus(PaymentStatus.SUCCESS);
        payment = paymentRepository.save(payment);

        log.info("Paiement SUCCESS ID={}, montant débité", payment.getId());

        invoiceService.generateInvoice(payment, request.getEmail());
        notificationService.sendPaymentSuccessNotification(payment, request.getEmail());
        subscriptionCallbackService.sendPaymentConfirmation(payment);

        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .transactionToken(payment.getTransactionToken())
                .status(payment.getStatus().name())
                .message("Paiement validé avec succès")
                .invoiceId(null)
                .invoiceNumber(null)
                .failureReason(null)
                .build();
    }

    @Transactional(readOnly = true)
    public PaymentDetailsResponse getPaymentById(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BadRequestException("Paiement introuvable ID: " + paymentId));
        return paymentMapper.toDetailsResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentDetailsResponse> getPaymentsByUserId(Long userId) {
        return paymentRepository.findByUserId(userId)
                .stream()
                .map(paymentMapper::toDetailsResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public PaymentDetailsResponse cancelPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BadRequestException("Paiement introuvable ID: " + paymentId));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BadRequestException("Seul un paiement PENDING peut être annulé");
        }

        payment.setStatus(PaymentStatus.CANCELLED);
        payment = paymentRepository.save(payment);

        log.info("Paiement annulé ID={}", paymentId);

        return paymentMapper.toDetailsResponse(payment);
    }

    private PaymentResponse buildFailedResponse(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .transactionToken(payment.getTransactionToken())
                .status(payment.getStatus().name())
                .message("Paiement échoué")
                .invoiceId(null)
                .invoiceNumber(null)
                .failureReason(payment.getFailureReason() != null ? payment.getFailureReason().name() : null)
                .build();
    }
}