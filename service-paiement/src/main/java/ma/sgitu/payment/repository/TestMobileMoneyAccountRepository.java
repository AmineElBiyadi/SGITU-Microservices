package ma.sgitu.payment.repository;

import ma.sgitu.payment.entity.TestMobileMoneyAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TestMobileMoneyAccountRepository extends JpaRepository<TestMobileMoneyAccount, Long> {
    Optional<TestMobileMoneyAccount> findByPhoneHash(String phoneHash);
    Optional<TestMobileMoneyAccount> findByMaskedPhone(String maskedPhone);
}