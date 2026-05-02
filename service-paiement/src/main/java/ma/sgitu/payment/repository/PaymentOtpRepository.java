package ma.sgitu.payment.repository;

import ma.sgitu.payment.entity.PaymentOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentOtpRepository extends JpaRepository<PaymentOtp, Long> {
    Optional<PaymentOtp> findTopByPaymentAccountIdAndStatusOrderByCreatedAtDesc(
            Long paymentAccountId, String status
    );
}