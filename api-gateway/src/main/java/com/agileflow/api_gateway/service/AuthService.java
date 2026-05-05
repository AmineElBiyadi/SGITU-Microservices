package com.agileflow.api_gateway.service;

import com.agileflow.api_gateway.dto.AuthResponse;
import com.agileflow.api_gateway.dto.LoginRequest;
import com.agileflow.api_gateway.dto.RegisterRequest;
import com.agileflow.api_gateway.model.User;
import com.agileflow.api_gateway.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenService tokenService;

    // ────────────────────────────────────────────────
    //  Register
    // ────────────────────────────────────────────────

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email déjà utilisé : " + request.getEmail());
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        if (request.getRole() != null && !request.getRole().isBlank()) {
            user.setRole(User.RoleType.valueOf(request.getRole()));
        } else {
            user.setRole(User.RoleType.ROLE_USER);
        }

        userRepository.save(user);

        String accessToken  = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        tokenService.saveToken(user, refreshToken);

        return new AuthResponse(accessToken, refreshToken, "Bearer");
    }

    // ────────────────────────────────────────────────
    //  Login
    // ────────────────────────────────────────────────

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Email ou mot de passe incorrect"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Email ou mot de passe incorrect");
        }

        String accessToken  = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        tokenService.saveToken(user, refreshToken);

        return new AuthResponse(accessToken, refreshToken, "Bearer");
    }

    // ────────────────────────────────────────────────
    //  Logout
    // ────────────────────────────────────────────────

    public void logout(String refreshToken) {
        tokenService.revokeToken(refreshToken);
    }

    // ────────────────────────────────────────────────
    //  Refresh Token
    // ────────────────────────────────────────────────

    public AuthResponse refreshToken(String refreshToken) {
        if (!tokenService.isTokenValid(refreshToken)) {
            throw new RuntimeException("Refresh token invalide ou révoqué");
        }

        String email = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        String newAccessToken  = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);
        tokenService.saveToken(user, newRefreshToken);

        return new AuthResponse(newAccessToken, newRefreshToken, "Bearer");
    }
}