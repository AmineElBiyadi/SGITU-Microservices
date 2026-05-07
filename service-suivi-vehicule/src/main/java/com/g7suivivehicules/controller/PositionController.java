package com.g7suivivehicules.controller;

import com.g7suivivehicules.dto.PositionGPSRequest;
import com.g7suivivehicules.dto.PositionGPSResponse;
import com.g7suivivehicules.service.PositionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/suivi-vehicules/positions")
@RequiredArgsConstructor
@Tag(name = "Positions GPS", description = "Ingestion des données GPS depuis les capteurs IoT et consultation de l'historique. Chaque position reçue déclenche automatiquement l'analyse d'anomalies.")
public class PositionController {

    private final PositionService positionService;

    @PostMapping
    @Operation(
            summary = "Enregistrer une position GPS",
            description = "Reçoit les données GPS d'un capteur IoT embarqué dans le véhicule. " +
                    "Déclenche automatiquement la détection d'anomalies (vitesse excessive, déviation d'itinéraire, " +
                    "retard horaire, freinage brusque, immobilisation). " +
                    "Crée ou résout des alertes en base de données selon les seuils configurés."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Position enregistrée et analyse déclenchée"),
            @ApiResponse(responseCode = "400", description = "Données invalides — vehiculeId, latitude ou longitude manquant"),
            @ApiResponse(responseCode = "500", description = "Erreur interne lors de l'enregistrement")
    })
    public ResponseEntity<PositionGPSResponse> enregistrerPosition(
            @Valid @RequestBody PositionGPSRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(positionService.enregistrerPosition(request));
    }

    @GetMapping
    @Operation(summary = "Toutes les positions GPS", description = "Retourne l'ensemble des positions enregistrées, triées par date décroissante.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des positions retournée")
    })
    public ResponseEntity<List<PositionGPSResponse>> getToutesLesPositions() {
        return ResponseEntity.ok(positionService.getToutesLesPositions());
    }

    @GetMapping("/{vehiculeId}")
    @Operation(summary = "Position actuelle d'un véhicule", description = "Retourne la dernière position GPS connue du véhicule.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dernière position trouvée"),
            @ApiResponse(responseCode = "404", description = "Aucune position enregistrée pour ce véhicule")
    })
    public ResponseEntity<PositionGPSResponse> getPositionActuelle(
            @Parameter(description = "UUID du véhicule", example = "53c31262-591a-44d4-8872-51e84611ac5e")
            @PathVariable UUID vehiculeId) {
        return ResponseEntity.ok(positionService.getPositionActuelle(vehiculeId));
    }

    @GetMapping("/{vehiculeId}/historique")
    @Operation(summary = "Historique des positions", description = "Retourne toutes les positions enregistrées pour un véhicule, triées par date décroissante.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historique retourné"),
            @ApiResponse(responseCode = "404", description = "Véhicule introuvable")
    })
    public ResponseEntity<List<PositionGPSResponse>> getHistorique(
            @Parameter(description = "UUID du véhicule", example = "53c31262-591a-44d4-8872-51e84611ac5e")
            @PathVariable UUID vehiculeId) {
        return ResponseEntity.ok(positionService.getHistorique(vehiculeId));
    }

    @GetMapping("/{vehiculeId}/vitesse-moyenne")
    @Operation(summary = "Vitesse moyenne", description = "Calcule et retourne la vitesse moyenne en km/h sur l'ensemble des positions enregistrées pour ce véhicule.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vitesse moyenne calculée (en km/h)"),
            @ApiResponse(responseCode = "404", description = "Aucune donnée disponible")
    })
    public ResponseEntity<Double> getVitesseMoyenne(
            @Parameter(description = "UUID du véhicule", example = "53c31262-591a-44d4-8872-51e84611ac5e")
            @PathVariable UUID vehiculeId) {
        return ResponseEntity.ok(positionService.calculerVitesseMoyenne(vehiculeId));
    }

    @GetMapping("/{vehiculeId}/retard")
    @Operation(summary = "Retard en secondes", description = "Calcule le nombre de secondes écoulées depuis la dernière position GPS reçue. Utile pour détecter une perte de signal.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Retard en secondes"),
            @ApiResponse(responseCode = "404", description = "Aucune position disponible")
    })
    public ResponseEntity<Long> getRetard(
            @Parameter(description = "UUID du véhicule", example = "53c31262-591a-44d4-8872-51e84611ac5e")
            @PathVariable UUID vehiculeId) {
        return ResponseEntity.ok(positionService.calculerRetard(vehiculeId));
    }

    @DeleteMapping("/{vehiculeId}/historique")
    @Operation(summary = "Purger l'historique GPS", description = "Supprime toutes les positions GPS enregistrées pour un véhicule. Opération irréversible.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Historique supprimé"),
            @ApiResponse(responseCode = "404", description = "Véhicule introuvable")
    })
    public ResponseEntity<Void> supprimerHistorique(
            @Parameter(description = "UUID du véhicule", example = "53c31262-591a-44d4-8872-51e84611ac5e")
            @PathVariable UUID vehiculeId) {
        positionService.supprimerHistorique(vehiculeId);
        return ResponseEntity.noContent().build();
    }
}