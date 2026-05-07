package com.sgitu.userservice.controller;
import com.sgitu.userservice.dto.LoginRequestDTO;
import com.sgitu.userservice.dto.LoginResponseDTO;
import com.sgitu.userservice.entity.Role;
import com.sgitu.userservice.entity.User;
import com.sgitu.userservice.repository.UserRepository;
import com.sgitu.userservice.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "Connexion et emission de tokens JWT (G3 est l emetteur officiel)")
public class AuthController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Operation(summary = "Connexion utilisateur",
        description = "Valide les identifiants et retourne un JWT signe. G10 doit forwarder les requetes de login vers cet endpoint.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Authentification reussie, token JWT retourne"),
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
        String token = jwtService.generateToken(user.getId(), user.getEmail(), roles);
        return ResponseEntity.ok(LoginResponseDTO.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .roles(roles)
                .build());
    }
}