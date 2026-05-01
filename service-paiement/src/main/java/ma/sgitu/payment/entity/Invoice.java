package ma.sgitu.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import ma.sgitu.payment.enums.PaymentMethod;
import ma.sgitu.payment.enums.SourceType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String invoiceNumber;
    @OneToOne
    @JoinColumn(name = "payment_id")
    private Payment payment;
    private Long userId;
    @Enumerated(EnumType.STRING)
    private SourceType sourceType;
    private Long sourceId;
    private BigDecimal totalAmount;
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;
    private LocalDateTime issuedAt;
}