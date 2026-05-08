package ma.sgitu.payment.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.sgitu.payment.dto.response.InvoiceResponse;
import ma.sgitu.payment.service.InvoiceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Slf4j
public class InvoiceController {

    private final InvoiceService invoiceService;

    // ✅ Récupérer facture par ID
    @GetMapping("/invoices/{invoiceId}")
    public ResponseEntity<InvoiceResponse> getInvoiceById(
            @PathVariable Long invoiceId) {

        log.info("GET /invoices/{} appelé", invoiceId);
        return ResponseEntity.ok(invoiceService.getInvoiceById(invoiceId));
    }

    // ✅ Récupérer facture par numéro
    @GetMapping("/invoices/number/{invoiceNumber}")
    public ResponseEntity<InvoiceResponse> getInvoiceByNumber(
            @PathVariable String invoiceNumber) {

        log.info("GET /invoices/number/{} appelé", invoiceNumber);
        return ResponseEntity.ok(invoiceService.getInvoiceByNumber(invoiceNumber));
    }

    // ✅ Facture liée à un paiement
    @GetMapping("/payments/{paymentId}/invoice")
    public ResponseEntity<InvoiceResponse> getInvoiceByPaymentId(
            @PathVariable Long paymentId) {

        log.info("GET /payments/{}/invoice appelé", paymentId);
        return ResponseEntity.ok(invoiceService.getInvoiceByPaymentId(paymentId));
    }

    // ✅ Factures d’un utilisateur
    @GetMapping("/invoices/user/{userId}")
    public ResponseEntity<List<InvoiceResponse>> getInvoicesByUserId(
            @PathVariable Long userId) {

        log.info("GET /invoices/user/{} appelé", userId);
        return ResponseEntity.ok(invoiceService.getInvoicesByUserId(userId));
    }

    // ✅ Health check
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("G6 Payment Service - UP");
    }
}