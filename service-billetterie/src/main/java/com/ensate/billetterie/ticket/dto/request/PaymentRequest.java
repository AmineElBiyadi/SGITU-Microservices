package com.ensate.billetterie.ticket.dto.request;


import com.ensate.billetterie.ticket.domain.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PaymentRequest {
    @NotBlank
    private String userId;
    private String sourceType = "TICKET";
    private PaymentMethod paymentMethod;
    private String savedPaymentToken;
}
