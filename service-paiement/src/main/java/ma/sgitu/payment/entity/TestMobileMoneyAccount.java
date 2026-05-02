package ma.sgitu.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import ma.sgitu.payment.enums.AccountStatus;
import java.math.BigDecimal;

@Entity
@Table(name = "test_mobile_money_accounts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TestMobileMoneyAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String phoneHash; // Le numéro haché pour la recherche

    private String maskedPhone; // ex: 0612****78 pour l'affichage

    private String provider; // INWI, ORANGE, IAM

    private BigDecimal balance; // Solde simulé

    @Enumerated(EnumType.STRING)
    private AccountStatus status; // ACTIVE ou BLOCKED
}