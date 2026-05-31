package ma.sgitu.payment.repository;

import ma.sgitu.payment.entity.PaymentOtp;
import ma.sgitu.payment.enums.OtpStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentOtpRepository extends JpaRepository<PaymentOtp, Long> {

    List<PaymentOtp> findByPaymentAccountIdAndStatus(Long paymentAccountId, OtpStatus status);
}