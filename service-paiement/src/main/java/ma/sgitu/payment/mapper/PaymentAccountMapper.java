package ma.sgitu.payment.mapper;

import ma.sgitu.payment.dto.response.PaymentAccountResponse;
import ma.sgitu.payment.entity.PaymentAccount;
import org.springframework.stereotype.Component;

@Component
public class PaymentAccountMapper {

    public PaymentAccountResponse toResponse(PaymentAccount account) {
        return PaymentAccountResponse.builder()
                .id(account.getId())
                .userId(account.getUserId())
                .paymentMethod(account.getPaymentMethod())
                .paymentToken(account.getPaymentToken())
                .maskedIdentifier(account.getMaskedIdentifier())
                .provider(account.getProvider())
                .balance(account.getBalance())
                .status(account.getStatus())
                .expiryMonth(account.getExpiryMonth())
                .expiryYear(account.getExpiryYear())
                .createdAt(account.getCreatedAt())
                .build();
    }
}