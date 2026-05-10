package com.agileflow.api_gateway.service;

import com.agileflow.api_gateway.model.Token;
import com.agileflow.api_gateway.model.User;
import com.agileflow.api_gateway.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final TokenRepository tokenRepository;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    public Token saveToken(User user, String refreshToken) {
        revokeAllUserTokens(user);

        Token token = new Token();
        token.setUser(user);
        token.setRefreshToken(refreshToken);
        token.setRefreshTokenExpiry(LocalDateTime.now().plusSeconds(refreshExpiration / 1000));
        token.setRevoked(false);

        return tokenRepository.save(token);
    }

    public void revokeToken(String refreshToken) {
        tokenRepository.findByRefreshToken(refreshToken).ifPresent(token -> {
            token.setRevoked(true);
            tokenRepository.save(token);
        });
    }

    public void revokeAllUserTokens(User user) {
        tokenRepository.findAllByUser(user).forEach(token -> {
            token.setRevoked(true);
            tokenRepository.save(token);
        });
    }

    public Optional<Token> findByRefreshToken(String refreshToken) {
        return tokenRepository.findByRefreshToken(refreshToken);
    }

    public boolean isTokenValid(String refreshToken) {
        return tokenRepository.findByRefreshToken(refreshToken)
                .map(token -> !token.isRevoked()
                        && token.getRefreshTokenExpiry().isAfter(LocalDateTime.now()))
                .orElse(false);
    }
}
