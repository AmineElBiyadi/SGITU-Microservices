package com.agileflow.api_gateway.repository;

import com.agileflow.api_gateway.model.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Long> {
    Optional<Token> findByRefreshToken(String refreshToken);
    void deleteByUserId(Long userId);
}