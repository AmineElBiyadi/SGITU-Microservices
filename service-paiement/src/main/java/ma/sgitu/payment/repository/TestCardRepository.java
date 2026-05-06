package ma.sgitu.payment.repository;

import ma.sgitu.payment.entity.TestCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository pour les cartes de test
 */
@Repository
public interface TestCardRepository extends JpaRepository<TestCard, Long> {

    /**
     * Trouve une carte par le hash de son numéro
     * @param cardNumberHash Hash du numéro de carte
     * @return Optional<TestCard>
     */
    Optional<TestCard> findByCardNumberHash(String cardNumberHash);

    /**
     * Vérifie si une carte existe par son hash
     * @param cardNumberHash Hash du numéro de carte
     * @return boolean
     */
    boolean existsByCardNumberHash(String cardNumberHash);
}