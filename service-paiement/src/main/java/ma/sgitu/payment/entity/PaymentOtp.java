package ma.sgitu.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import ma.sgitu.payment.enums.OtpStatus;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_otps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private Long paymentAccountId;

    private String otpHash;

    @Enumerated(EnumType.STRING)
    private OtpStatus status;

    private LocalDateTime expiresAt;

    private Integer attempts;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime verifiedAt;
}