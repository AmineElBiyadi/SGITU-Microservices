package ma.sgitu.payment.repository;

import ma.sgitu.payment.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {
    Optional<Refund> findByRefundToken(String refundToken);
    List<Refund> findByPaymentId(Long paymentId);
    List<Refund> findByUserId(Long userId);
}