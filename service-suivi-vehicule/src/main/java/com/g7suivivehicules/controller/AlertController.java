package com.g7suivivehicules.controller;

import com.g7suivivehicules.dto.AlertResponseDTO;
import com.g7suivivehicules.dto.AlertStatsDTO;
import com.g7suivivehicules.entity.Alert;
import com.g7suivivehicules.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/suivi-vehicules/alerts")
@RequiredArgsConstructor
@Tag(
        name = "Alertes",
        description = "Gestion du cycle de vie des alertes d'anomalies détectées automatiquement. " +
                "Types : VITESSE_EXCESSIVE, TEMPERATURE_CRITIQUE, CARBURANT_CRITIQUE, " +
                "FREINAGE_BRUSQUE, IMMOBILISATION, DEVIATION_ITINERAIRE, RETARD_HORAIRE. " +
                "Statuts : OUVERTE → RESOLUE (auto) ou ANNULEE (manuel opérateur)."
)
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    @Operation(
            summary = "Lister les alertes avec filtres",
            description = "Retourne les alertes filtrées par vehiculeId, statut et/ou typeAlert. " +
                    "Tous les paramètres sont optionnels. Sans paramètre, retourne toutes les alertes."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste d'alertes retournée"),
            @ApiResponse(responseCode = "400", description = "Paramètre de filtre invalide (enum inconnu)")
    })
    public ResponseEntity<List<AlertResponseDTO>> listerAlertes(
            @Parameter(description = "Filtrer par UUID du véhicule", example = "53c31262-591a-44d4-8872-51e84611ac5e")
            @RequestParam(required = false) UUID vehiculeId,
            @Parameter(description = "Filtrer par statut : OUVERTE, RESOLUE, ANNULEE", example = "OUVERTE")
            @RequestParam(required = false) Alert.StatutAlert statut,
            @Parameter(description = "Filtrer par type d'alerte", example = "VITESSE_EXCESSIVE")
            @RequestParam(required = false) Alert.TypeAlert typeAlert) {

        List<AlertResponseDTO> responses = alertService.listerAvecFiltres(vehiculeId, statut, typeAlert)
                .stream()
                .map(AlertResponseDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une alerte", description = "Retourne toutes les informations d'une alerte spécifique par son UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alerte trouvée"),
            @ApiResponse(responseCode = "404", description = "Alerte introuvable avec cet ID")
    })
    public ResponseEntity<AlertResponseDTO> obtenirAlerte(
            @Parameter(description = "UUID de l'alerte", example = "36b01ab6-31f8-47cb-82f4-5ffad4a90b95")
            @PathVariable UUID id) {
        return ResponseEntity.ok(AlertResponseDTO.fromEntity(alertService.trouverParId(id)));
    }

    @GetMapping("/active")
    @Operation(
            summary = "Toutes les alertes OUVERTES",
            description = "Retourne uniquement les alertes en cours (statut OUVERTE), " +
                    "toutes catégories et tous véhicules confondus. " +
                    "Endpoint principal pour le tableau de bord de supervision."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des alertes actives (peut être vide si aucune anomalie)")
    })
    public ResponseEntity<List<AlertResponseDTO>> listerAlertesActives() {
        List<AlertResponseDTO> responses = alertService.listerActives()
                .stream()
                .map(AlertResponseDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/vehicule/{vehiculeId}")
    @Operation(
            summary = "Historique des alertes d'un véhicule",
            description = "Retourne toutes les alertes (OUVERTES, RESOLUES et ANNULEES) pour un véhicule donné, " +
                    "triées par date de création décroissante."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historique des alertes retourné"),
            @ApiResponse(responseCode = "404", description = "Véhicule introuvable")
    })
    public ResponseEntity<List<AlertResponseDTO>> listerParVehicule(
            @Parameter(description = "UUID du véhicule", example = "53c31262-591a-44d4-8872-51e84611ac5e")
            @PathVariable UUID vehiculeId) {
        List<AlertResponseDTO> responses = alertService.listerParVehicule(vehiculeId)
                .stream()
                .map(AlertResponseDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/vehicule/{vehiculeId}/active")
    @Operation(
            summary = "Alertes OUVERTES d'un véhicule",
            description = "Retourne uniquement les alertes en cours pour un véhicule spécifique. " +
                    "Utile pour vérifier si un véhicule est actuellement en anomalie."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alertes actives du véhicule (peut être vide)"),
            @ApiResponse(responseCode = "404", description = "Véhicule introuvable")
    })
    public ResponseEntity<List<AlertResponseDTO>> listerActivesParVehicule(
            @Parameter(description = "UUID du véhicule", example = "53c31262-591a-44d4-8872-51e84611ac5e")
            @PathVariable UUID vehiculeId) {
        List<AlertResponseDTO> responses = alertService.listerActivesParVehicule(vehiculeId)
                .stream()
                .map(AlertResponseDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}/cancel")
    @Operation(
            summary = "Annuler une alerte (fausse alerte)",
            description = "Action manuelle opérateur. Passe le statut de l'alerte à ANNULEE. " +
                    "À utiliser quand une alerte est une fausse détection. " +
                    "L'alerte n'est pas supprimée — elle reste dans l'historique."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alerte annulée, statut = ANNULEE"),
            @ApiResponse(responseCode = "404", description = "Alerte introuvable"),
            @ApiResponse(responseCode = "400", description = "Alerte déjà RESOLUE ou ANNULEE")
    })
    public ResponseEntity<AlertResponseDTO> annulerAlerte(
            @Parameter(description = "UUID de l'alerte à annuler", example = "36b01ab6-31f8-47cb-82f4-5ffad4a90b95")
            @PathVariable UUID id) {
        return ResponseEntity.ok(AlertResponseDTO.fromEntity(alertService.annuler(id)));
    }

    @GetMapping("/stats")
    @Operation(
            summary = "Statistiques des alertes",
            description = "Agrégats pour le Groupe 8 (Statistiques). " +
                    "Retourne le nombre total d'alertes, la répartition par type (VITESSE_EXCESSIVE, etc.) " +
                    "et par statut (OUVERTE, RESOLUE, ANNULEE)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statistiques calculées avec succès")
    })
    public ResponseEntity<AlertStatsDTO> obtenirStats() {
        Map<String, Long> parType = alertService.statsParType().stream()
                .collect(Collectors.toMap(
                        row -> row[0].toString(),
                        row -> (Long) row[1]
                ));

        Map<String, Long> parStatut = alertService.statsParStatut().stream()
                .collect(Collectors.toMap(
                        row -> row[0].toString(),
                        row -> (Long) row[1]
                ));

        long total = parStatut.values().stream().mapToLong(Long::longValue).sum();

        AlertStatsDTO stats = AlertStatsDTO.builder()
                .parType(parType)
                .parStatut(parStatut)
                .totalAlertes(total)
                .build();

        return ResponseEntity.ok(stats);
    }
}
