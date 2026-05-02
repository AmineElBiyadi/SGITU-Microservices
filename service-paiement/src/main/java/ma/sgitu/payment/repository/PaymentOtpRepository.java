package ma.sgitu.payment.repository;

import ma.sgitu.payment.entity.PaymentOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentOtpRepository extends JpaRepository<PaymentOtp, Long> {
}