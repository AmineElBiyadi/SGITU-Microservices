package com.ensate.billetterie.ticket.dto.request;


import com.ensate.billetterie.ticket.domain.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Email;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentRequest {
    @NotBlank
    private String userId;
    private String sourceType = "TICKET";
    private String sourceId;
    @NotNull
    @Positive
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private String savedPaymentToken;
    @NotBlank
    @Email
    private String email;
}
