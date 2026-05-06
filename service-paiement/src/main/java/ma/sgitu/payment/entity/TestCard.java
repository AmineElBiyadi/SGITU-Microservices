package ma.sgitu.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import ma.sgitu.payment.enums.AccountStatus;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity TestCard - Carte fictive pour simulation bancaire
 * Table : test_cards
 */
@Entity
@Table(name = "test_cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_number_hash", nullable = false, unique = true)
    private String cardNumberHash;

    @Column(name = "cvv_hash", nullable = false)
    private String cvvHash;

    @Column(name = "last4", nullable = false, length = 4)
    private String last4;

    @Column(name = "card_holder_name", nullable = false, length = 100)
    private String cardHolderName;

    @Column(name = "expiry_month", nullable = false)
    private Integer expiryMonth;

    @Column(name = "expiry_year", nullable = false)
    private Integer expiryYear;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "balance", nullable = false, precision = 10, scale = 2)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AccountStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}