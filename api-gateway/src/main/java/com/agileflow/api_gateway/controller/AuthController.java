package com.agileflow.api_gateway.controller;

import com.agileflow.api_gateway.dto.AuthResponse;
import com.agileflow.api_gateway.dto.LoginRequest;
import com.agileflow.api_gateway.dto.RegisterRequest;
import com.agileflow.api_gateway.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ────────────────────────────────────────────────
    //  POST /auth/register
    // ────────────────────────────────────────────────

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    // ────────────────────────────────────────────────
    //  POST /auth/login
    // ────────────────────────────────────────────────

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // ────────────────────────────────────────────────
    //  POST /auth/logout
    //  Body : { "refreshToken": "..." }
    // ────────────────────────────────────────────────

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestBody Map<String, String> body) {
        authService.logout(body.get("refreshToken"));
        return ResponseEntity.ok(Map.of("message", "Déconnexion réussie"));
    }

    // ────────────────────────────────────────────────
    //  POST /auth/refresh
    //  Body : { "refreshToken": "..." }
    // ────────────────────────────────────────────────

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(authService.refreshToken(body.get("refreshToken")));
    }
}