package ma.sgitu.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.sgitu.payment.dto.request.RefundRequest;
import ma.sgitu.payment.dto.response.RefundResponse;
import ma.sgitu.payment.entity.Payment;
import ma.sgitu.payment.entity.PaymentAccount;
import ma.sgitu.payment.entity.Refund;
import ma.sgitu.payment.enums.PaymentStatus;
import ma.sgitu.payment.enums.RefundStatus;
import ma.sgitu.payment.enums.SourceType;
import ma.sgitu.payment.exception.BadRequestException;
import ma.sgitu.payment.repository.PaymentAccountRepository;
import ma.sgitu.payment.repository.PaymentRepository;
import ma.sgitu.payment.repository.RefundRepository;
import ma.sgitu.payment.util.TokenGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundService {

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentAccountRepository paymentAccountRepository;
    private final SubscriptionCallbackService subscriptionCallbackService;

    @Transactional
    public RefundResponse processRefund(Long paymentId, RefundRequest request) {
        log.info("Demande de remboursement pour paiement ID: {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BadRequestException("Paiement introuvable ID: " + paymentId));

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new BadRequestException("Seul un paiement SUCCESS peut être remboursé");
        }

        if (request.getAmount().compareTo(payment.getAmount()) > 0) {
            throw new BadRequestException("Le montant du remboursement ne peut pas dépasser le montant du paiement");
        }

        String refundToken = "REFUND-TOKEN-" + System.currentTimeMillis();

        Refund refund = Refund.builder()
                .refundToken(refundToken)
                .paymentId(paymentId)
                .userId(payment.getUserId())
                .amount(request.getAmount())
                .status(RefundStatus.REFUND_PENDING)
                .reason(request.getReason())
                .build();

        refund = refundRepository.save(refund);

        PaymentAccount account = paymentAccountRepository.findByPaymentToken(payment.getSavedPaymentToken())
                .orElse(null);

        if (account == null) {
            refund.setStatus(RefundStatus.REFUND_FAILED);
            refund.setFailureReason("PAYMENT_ACCOUNT_NOT_FOUND");
            refundRepository.save(refund);
            return toFailedResponse(refund, "Compte de paiement introuvable");
        }

        account.setBalance(account.getBalance().add(request.getAmount()));
        paymentAccountRepository.save(account);

        refund.setStatus(RefundStatus.REFUNDED);
        refund = refundRepository.save(refund);

        log.info("Remboursement SUCCESS ID={}, montant crédité", refund.getId());

        if (payment.getSourceType() == SourceType.SUBSCRIPTION) {
            subscriptionCallbackService.sendRefundConfirmation(payment, refund);
        }

        return toSuccessResponse(refund);
    }

    @Transactional(readOnly = true)
    public RefundResponse getRefundById(Long refundId) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new BadRequestException("Remboursement introuvable ID: " + refundId));
        return toResponse(refund);
    }

    @Transactional(readOnly = true)
    public List<RefundResponse> getRefundsByPaymentId(Long paymentId) {
        return refundRepository.findByPaymentId(paymentId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RefundResponse> getRefundsByUserId(Long userId) {
        return refundRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private RefundResponse toResponse(Refund refund) {
        return RefundResponse.builder()
                .refundId(refund.getId())
                .refundToken(refund.getRefundToken())
                .paymentId(refund.getPaymentId())
                .userId(refund.getUserId())
                .amountRefunded(refund.getAmount())
                .status(refund.getStatus().name())
                .reason(refund.getReason())
                .failureReason(refund.getFailureReason())
                .createdAt(refund.getCreatedAt())
                .build();
    }

    private RefundResponse toSuccessResponse(Refund refund) {
        return RefundResponse.builder()
                .refundId(refund.getId())
                .refundToken(refund.getRefundToken())
                .paymentId(refund.getPaymentId())
                .status(refund.getStatus().name())
                .amountRefunded(refund.getAmount())
                .message("Remboursement effectué avec succès")
                .failureReason(null)
                .createdAt(refund.getCreatedAt())
                .build();
    }

    private RefundResponse toFailedResponse(Refund refund, String message) {
        return RefundResponse.builder()
                .refundId(refund.getId())
                .refundToken(refund.getRefundToken())
                .paymentId(refund.getPaymentId())
                .status(refund.getStatus().name())
                .amountRefunded(java.math.BigDecimal.ZERO)
                .message(message)
                .failureReason(refund.getFailureReason())
                .createdAt(refund.getCreatedAt())
                .build();
    }
}