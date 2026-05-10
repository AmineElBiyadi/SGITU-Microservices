package ma.sgitu.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.sgitu.payment.dto.response.InvoiceResponse;
import ma.sgitu.payment.entity.Invoice;
import ma.sgitu.payment.entity.Payment;
import ma.sgitu.payment.enums.PaymentStatus;
import ma.sgitu.payment.exception.BadRequestException;
import ma.sgitu.payment.mapper.InvoiceMapper;
import ma.sgitu.payment.repository.InvoiceRepository;
import ma.sgitu.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final InvoiceMapper invoiceMapper;
    private final NotificationService notificationService;

    @Transactional
    public InvoiceResponse generateInvoice(Payment payment, String userEmail) {
        log.info("Génération facture pour paiement ID: {}", payment.getId());

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            log.error("Tentative de génération facture pour paiement non SUCCESS: {}", payment.getStatus());
            throw new BadRequestException("Impossible de générer une facture pour un paiement non réussi");
        }

        if (invoiceRepository.existsByPaymentId(payment.getId())) {
            log.warn("Facture déjà existante pour paiement ID: {}", payment.getId());
            return invoiceMapper.toResponse(
                    invoiceRepository.findByPaymentId(payment.getId())
                            .orElseThrow(() -> new RuntimeException("Erreur de cohérence facture"))
            );
        }

        String invoiceNumber = generateInvoiceNumber(payment.getId());

        Invoice invoice = Invoice.builder()
                .invoiceNumber(invoiceNumber)
                .payment(payment)
                .userId(payment.getUserId())
                .sourceType(payment.getSourceType())
                .sourceId(payment.getSourceId())
                .totalAmount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .build();

        invoice = invoiceRepository.save(invoice);
        log.info("Facture générée avec succès: {}", invoiceNumber);

        try {
            notificationService.sendInvoiceGeneratedNotification(invoice, userEmail);
        } catch (Exception e) {
            log.error("Échec notification INVOICE_GENERATED pour facture {}: {}", invoiceNumber, e.getMessage());
        }

        return invoiceMapper.toResponse(invoice);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceById(Long invoiceId) {
        log.info("Consultation facture ID: {}", invoiceId);

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BadRequestException("Facture introuvable avec ID: " + invoiceId));

        return invoiceMapper.toResponse(invoice);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceByNumber(String invoiceNumber) {
        log.info("Consultation facture numéro: {}", invoiceNumber);

        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new BadRequestException("Facture introuvable avec numéro: " + invoiceNumber));

        return invoiceMapper.toResponse(invoice);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceByPaymentId(Long paymentId) {
        log.info("Consultation facture pour paiement ID: {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BadRequestException("Paiement introuvable avec ID: " + paymentId));

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new BadRequestException("Aucune facture pour un paiement non réussi (statut: " + payment.getStatus() + ")");
        }

        Invoice invoice = invoiceRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new BadRequestException("Aucune facture générée pour ce paiement"));

        return invoiceMapper.toResponse(invoice);
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getInvoicesByUserId(Long userId) {
        log.info("Consultation factures utilisateur ID: {}", userId);

        List<Invoice> invoices = invoiceRepository.findByUserIdOrderByIssuedAtDesc(userId);

        return invoices.stream()
                .map(invoiceMapper::toResponse)
                .collect(Collectors.toList());
    }

    private String generateInvoiceNumber(Long paymentId) {
        return "INV-PAY-" + paymentId;
    }
}