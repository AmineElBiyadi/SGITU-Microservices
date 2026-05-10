package ma.sgitu.payment.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.sgitu.payment.dto.request.RefundRequest;
import ma.sgitu.payment.dto.response.RefundResponse;
import ma.sgitu.payment.service.RefundService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class RefundController {

    private final RefundService refundService;

    @PostMapping("/payments/{paymentId}/refund")
    public ResponseEntity<RefundResponse> processRefund(
            @PathVariable Long paymentId,
            @Valid @RequestBody RefundRequest request) {

        log.info("POST /payments/{}/refund appelé", paymentId);
        RefundResponse response = refundService.processRefund(paymentId, request);
        HttpStatus status = "REFUNDED".equals(response.getStatus())
                ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }

    @GetMapping("/refunds/{refundId}")
    public ResponseEntity<RefundResponse> getRefundById(
            @PathVariable Long refundId) {

        log.info("GET /refunds/{} appelé", refundId);
        return ResponseEntity.ok(refundService.getRefundById(refundId));
    }

    @GetMapping("/refunds/payment/{paymentId}")
    public ResponseEntity<List<RefundResponse>> getRefundsByPaymentId(
            @PathVariable Long paymentId) {

        log.info("GET /refunds/payment/{} appelé", paymentId);
        return ResponseEntity.ok(refundService.getRefundsByPaymentId(paymentId));
    }

    @GetMapping("/refunds/user/{userId}")
    public ResponseEntity<List<RefundResponse>> getRefundsByUserId(
            @PathVariable Long userId) {

        log.info("GET /refunds/user/{} appelé", userId);
        return ResponseEntity.ok(refundService.getRefundsByUserId(userId));
    }
}