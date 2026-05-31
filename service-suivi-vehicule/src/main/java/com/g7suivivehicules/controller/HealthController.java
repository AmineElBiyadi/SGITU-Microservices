package com.g7suivivehicules.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/suivi-vehicules")
@Tag(name = "Santé", description = "Endpoints de monitoring et santé du service")
public class HealthController {

    @GetMapping("/health")
    @Operation(summary = "Health check personnalisé pour G7")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "g7-suivi-vehicules",
            "version", "1.0.0"
        ));
    }
}
