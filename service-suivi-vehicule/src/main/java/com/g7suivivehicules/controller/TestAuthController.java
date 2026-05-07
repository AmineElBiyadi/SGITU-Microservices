package com.g7suivivehicules.controller;

import com.g7suivivehicules.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * CONTRÔLEUR DE TEST UNIQUEMENT.
 * À supprimer en production.
 */
@RestController
@RequestMapping("/api/auth/test")
@RequiredArgsConstructor
public class TestAuthController {

    private final JwtUtil jwtUtil;

    @GetMapping("/token")
    public Map<String, String> generateTestToken(@RequestParam(defaultValue = "admin") String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "ROLE_ADMIN"); // Format attendu par le filtre
        
        String token = jwtUtil.generateToken(username, claims);
        
        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        response.put("type", "Bearer");
        response.put("instructions", "Copiez ce token dans Postman > Authorization > Bearer Token");
        
        return response;
    }
}
