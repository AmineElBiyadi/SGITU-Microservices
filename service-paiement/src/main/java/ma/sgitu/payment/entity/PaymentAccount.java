package ma.sgitu.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import ma.sgitu.payment.enums.AccountStatus;
import ma.sgitu.payment.enums.PaymentMethod;
import java.math.BigDecimal;

@Entity
@Table(name = "payment_accounts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;
    private String paymentToken;
    private String maskedIdentifier;
    private String provider;
    private BigDecimal balance;
    @Enumerated(EnumType.STRING)
    private AccountStatus status;
}