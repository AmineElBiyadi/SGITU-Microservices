package com.agileflow.api_gateway.repository;

import com.agileflow.api_gateway.model.EmailVerificationToken;
import com.agileflow.api_gateway.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    List<EmailVerificationToken> findAllByUserAndUsedAtIsNull(User user);
}
