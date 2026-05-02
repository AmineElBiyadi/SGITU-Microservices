package ma.sgitu.payment.repository;

import ma.sgitu.payment.entity.TestCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TestCardRepository extends JpaRepository<TestCard, Long> {
    Optional<TestCard> findByCardNumberHash(String cardNumberHash);
}