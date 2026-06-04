package ma.sgitu.payment.repository;

import ma.sgitu.payment.entity.Payment;
import ma.sgitu.payment.enums.PaymentStatus;
import ma.sgitu.payment.enums.SourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByUserId(Long userId);
    Optional<Payment> findByTransactionToken(String transactionToken);
    Optional<Payment> findFirstBySourceTypeAndSourceIdAndStatusOrderByCreatedAtDesc(
            SourceType sourceType,
            String sourceId,
            PaymentStatus status
    );
    boolean existsBySourceTypeAndSourceId(SourceType sourceType, String sourceId);
}
