package com.sgitu.userservice.controller;
import com.sgitu.userservice.dto.LoginRequestDTO;
import com.sgitu.userservice.dto.LoginResponseDTO;
import com.sgitu.userservice.dto.RefreshRequestDTO;
import com.sgitu.userservice.entity.Role;
import com.sgitu.userservice.entity.User;
import com.sgitu.userservice.repository.UserRepository;
import com.sgitu.userservice.security.JwtService;
import com.sgitu.userservice.security.RedisTokenBlacklistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "Connexion, rafraichissement et emission de tokens JWT (G3 est l emetteur officiel)")
public class AuthController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RedisTokenBlacklistService blacklistService;

    @Operation(summary = "Connexion utilisateur",
        description = "Valide les identifiants et retourne un JWT d acces + un refresh token. G10 doit forwarder les requetes de login vers cet endpoint.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Authentification reussie, tokens retournes"),
        @ApiResponse(responseCode = "401", description = "Email ou mot de passe incorrect"),
        @ApiResponse(responseCode = "403", description = "Compte desactive")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect");
        }
        if (!user.getActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Compte desactive - contactez un administrateur");
        }
        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());
        String accessToken = jwtService.generateToken(user.getId(), user.getEmail(), roles);
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail(), roles);
        return ResponseEntity.ok(LoginResponseDTO.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .email(user.getEmail())
                .roles(roles)
                .build());
    }

    @Operation(summary = "Rafraichir le token d acces",
        description = "Echange un refresh token valide contre un nouveau couple access token + refresh token. L ancien refresh token est revoque (rotation).")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Nouveaux tokens generes"),
        @ApiResponse(responseCode = "401", description = "Refresh token invalide ou expire")
    })
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDTO> refresh(@Valid @RequestBody RefreshRequestDTO request) {
        String[] tokenData = jwtService.validateRefreshToken(request.getRefreshToken());
        if (tokenData == null || tokenData.length < 3) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token invalide ou expire");
        }

        // Révoquer l'ancien refresh token (rotation)
        jwtService.revokeRefreshToken(request.getRefreshToken());

        Long userId = Long.parseLong(tokenData[0]);
        String email = tokenData[1];
        List<String> roles = Arrays.asList(tokenData[2].split(","));

        String newAccessToken = jwtService.generateToken(userId, email, roles);
        String newRefreshToken = jwtService.generateRefreshToken(userId, email, roles);

        return ResponseEntity.ok(LoginResponseDTO.builder()
                .token(newAccessToken)
                .refreshToken(newRefreshToken)
                .userId(userId)
                .email(email)
                .roles(roles)
                .build());
    }

    @Operation(summary = "Déconnexion utilisateur",
        description = "Révoque le JWT courant et le refresh token associé.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Déconnexion réussie"),
        @ApiResponse(responseCode = "400", description = "Token invalide ou absent")
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request,
                                       @RequestBody(required = false) RefreshRequestDTO refreshRequest) {
        System.out.println("LOGOUT EXECUTED");
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Authorization header manquant ou incorrect");
        }

        String token = authHeader.substring(7);
        long ttlSeconds = jwtService.getTokenTtlSeconds(token);
        if (ttlSeconds <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token invalide ou expiré");
        }

        // Révoquer le access token
        blacklistService.revokeToken(token, Duration.ofSeconds(ttlSeconds));

        // Révoquer le refresh token si fourni
        if (refreshRequest != null && refreshRequest.getRefreshToken() != null) {
            jwtService.revokeRefreshToken(refreshRequest.getRefreshToken());
        }

        return ResponseEntity.noContent().build();
    }
}