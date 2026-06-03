package com.g7suivivehicules.controller;

import com.g7suivivehicules.dto.PositionGPSRequest;
import com.g7suivivehicules.service.G5NotificationService;
import com.g7suivivehicules.service.PositionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/suivi-vehicules/simulateur")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Simulateur", description = "Endpoints pour simuler des capteurs et des événements système")
public class SimulateurController {

    private final PositionService positionService;
    private final G5NotificationService g5NotificationService;

    @PostMapping("/sensor-data")
    @Operation(summary = "Simuler l'envoi de données capteur (Position GPS)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> simulateSensor(@RequestBody PositionGPSRequest request) {
        log.info("[Simulateur] Réception de données capteur simulées pour véhicule: {}", request.getVehiculeId());
        positionService.enregistrerPosition(request);
        return ResponseEntity.ok(Map.of("message", "Donnée capteur traitée avec succès"));
    }

    @PostMapping("/trigger-log-alert")
    @Operation(summary = "Simuler une erreur système pour tester les notifications logs G5")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> triggerLogAlert(@RequestBody Map<String, String> body) {
        String level = body.getOrDefault("level", "ERROR");
        String message = body.getOrDefault("message", "Erreur critique simulée par l'administrateur");
        
        log.info("[Simulateur] Déclenchement manuel d'une alerte log Admin via G5");
        g5NotificationService.notifierLogAdmin(level, message);
        
        return ResponseEntity.ok(Map.of(
            "status", "Notification envoyée",
            "target", "G5 Admin Notification",
            "level", level
        ));
    }
}
