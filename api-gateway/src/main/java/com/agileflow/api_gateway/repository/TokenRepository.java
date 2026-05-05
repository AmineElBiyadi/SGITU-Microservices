package com.agileflow.api_gateway.repository;

import com.agileflow.api_gateway.model.Token;
import com.agileflow.api_gateway.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {

    Optional<Token> findByRefreshToken(String refreshToken);

    List<Token> findAllByUser(User user);
}