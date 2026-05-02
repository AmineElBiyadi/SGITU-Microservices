package ma.sgitu.payment.repository;

import ma.sgitu.payment.entity.PaymentAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentAccountRepository extends JpaRepository<PaymentAccount, Long> {
    List<PaymentAccount> findByUserId(Long userId);
    Optional<PaymentAccount> findByPaymentToken(String paymentToken);
}