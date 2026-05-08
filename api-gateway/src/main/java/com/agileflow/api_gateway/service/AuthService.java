package com.agileflow.api_gateway.service;

import com.agileflow.api_gateway.dto.AuthResponse;
import com.agileflow.api_gateway.dto.ForgotPasswordRequest;
import com.agileflow.api_gateway.dto.LoginRequest;
import com.agileflow.api_gateway.dto.MessageResponse;
import com.agileflow.api_gateway.dto.RegisterRequest;
import com.agileflow.api_gateway.dto.ResetPasswordRequest;
import com.agileflow.api_gateway.model.EmailVerificationToken;
import com.agileflow.api_gateway.model.PasswordResetToken;
import com.agileflow.api_gateway.model.User;
import com.agileflow.api_gateway.repository.EmailVerificationTokenRepository;
import com.agileflow.api_gateway.repository.PasswordResetTokenRepository;
import com.agileflow.api_gateway.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenService tokenService;
    private final SecureTokenService secureTokenService;
    private final NotificationClient notificationClient;

    @Value("${g10.email.verification-expiration-minutes}")
    private long emailVerificationExpirationMinutes;

    @Value("${g10.email.password-reset-expiration-minutes}")
    private long passwordResetExpirationMinutes;

    public MessageResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email deja utilise");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(false);
        user.setEmailVerified(false);

        if (request.getRole() != null && !request.getRole().isBlank()) {
            user.setRole(User.RoleType.valueOf(request.getRole()));
        } else {
            user.setRole(User.RoleType.ROLE_USER);
        }

        userRepository.save(user);
        createEmailVerificationToken(user);

        return new MessageResponse("Compte cree. Verifiez votre email avant de vous connecter.");
    }

    public MessageResponse verifyEmail(String rawToken) {
        String tokenHash = secureTokenService.hashToken(rawToken);
        EmailVerificationToken token = emailVerificationTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token de verification invalide"));

        if (token.getUsedAt() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token de verification deja utilise");
        }

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token de verification expire");
        }

        User user = token.getUser();
        user.setEmailVerified(true);
        user.setEnabled(true);
        token.setUsedAt(LocalDateTime.now());

        userRepository.save(user);
        emailVerificationTokenRepository.save(token);

        return new MessageResponse("Email verifie. Vous pouvez maintenant vous connecter.");
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Email ou mot de passe incorrect"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Email ou mot de passe incorrect");
        }

        if (!user.isEmailVerified() || !user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Compte non active. Verifiez votre email.");
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        tokenService.saveToken(user, refreshToken);

        return new AuthResponse(accessToken, refreshToken, "Bearer");
    }

    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail())
                .filter(user -> user.isEnabled() && user.isEmailVerified())
                .ifPresent(this::createPasswordResetToken);

        return new MessageResponse("Si l'email existe, un lien de reinitialisation sera envoye.");
    }

    public MessageResponse resetPassword(ResetPasswordRequest request) {
        String tokenHash = secureTokenService.hashToken(request.getToken());
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token de reinitialisation invalide"));

        if (token.getUsedAt() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token de reinitialisation deja utilise");
        }

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token de reinitialisation expire");
        }

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        token.setUsedAt(LocalDateTime.now());

        userRepository.save(user);
        passwordResetTokenRepository.save(token);
        tokenService.revokeAllUserTokens(user);

        return new MessageResponse("Mot de passe reinitialise avec succes.");
    }

    public void logout(String refreshToken) {
        tokenService.revokeToken(refreshToken);
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (!tokenService.isTokenValid(refreshToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token invalide ou revoque");
        }

        String email = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable"));

        if (!user.isEnabled() || !user.isEmailVerified()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Compte non actif");
        }

        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);
        tokenService.saveToken(user, newRefreshToken);

        return new AuthResponse(newAccessToken, newRefreshToken, "Bearer");
    }

    private void createEmailVerificationToken(User user) {
        emailVerificationTokenRepository.findAllByUserAndUsedAtIsNull(user)
                .forEach(token -> token.setUsedAt(LocalDateTime.now()));

        String rawToken = secureTokenService.generateRawToken();
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setTokenHash(secureTokenService.hashToken(rawToken));
        token.setExpiresAt(LocalDateTime.now().plusMinutes(emailVerificationExpirationMinutes));

        emailVerificationTokenRepository.save(token);
        notificationClient.sendVerificationEmail(user.getEmail(), rawToken);
    }

    private void createPasswordResetToken(User user) {
        passwordResetTokenRepository.findAllByUserAndUsedAtIsNull(user)
                .forEach(token -> token.setUsedAt(LocalDateTime.now()));

        String rawToken = secureTokenService.generateRawToken();
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(secureTokenService.hashToken(rawToken));
        token.setExpiresAt(LocalDateTime.now().plusMinutes(passwordResetExpirationMinutes));

        passwordResetTokenRepository.save(token);
        notificationClient.sendPasswordResetEmail(user.getEmail(), rawToken);
    }
}
