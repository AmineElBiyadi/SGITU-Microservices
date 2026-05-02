package ma.sgitu.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_otps")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentOtp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long paymentAccountId;
    private String otpHash; // Code haché (Sécurité)

    private String status; // PENDING, VERIFIED, EXPIRED, FAILED
    private LocalDateTime expiresAt;
    private Integer attempts; // Nombre de tentatives (max 3 par exemple)
    private LocalDateTime createdAt;
}