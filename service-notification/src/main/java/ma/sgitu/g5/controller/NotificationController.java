package ma.sgitu.g5.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.sgitu.g5.dto.request.NotificationRequestDTO;
import ma.sgitu.g5.dto.response.NotificationResponseDTO;
import ma.sgitu.g5.entity.Notification;
import ma.sgitu.g5.repository.NotificationRepository;
import ma.sgitu.g5.service.INotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Validated
@Tag(name = "Notifications", description = "API centrale du microservice G5 - Gestion des notifications Email, SMS et Push")
public class NotificationController {

    private final INotificationService notificationService;
    private final NotificationRepository notificationRepository;

    @PostMapping("/send")
    @Operation(
            summary = "Envoyer une notification",
            description = "Envoie une notification via EMAIL, SMS, PUSH ou LOG (admin). Retourne immédiatement 202 QUEUED, l'envoi est asynchrone."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Notification acceptée et en file d'attente (QUEUED) ou déjà traitée (ALREADY_QUEUED)"),
            @ApiResponse(responseCode = "400", description = "Canal non supporté ou destinataire manquant"),
            @ApiResponse(responseCode = "401", description = "JWT manquant ou invalide")
    })
    public ResponseEntity<NotificationResponseDTO> send(
            @Valid @RequestBody NotificationRequestDTO dto) {

        validateChannelAndRecipient(dto);
        NotificationResponseDTO response = notificationService.send(dto);
        return ResponseEntity.accepted().body(response);
    }

    @GetMapping("/{notificationId}")
    @Operation(
            summary = "Consulter le statut d'une notification",
            description = "Retourne les détails et le statut actuel d'une notification par son ID"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification trouvée"),
            @ApiResponse(responseCode = "404", description = "Notification introuvable")
    })
    public ResponseEntity<Notification> getById(
            @PathVariable String notificationId) {
        Notification n = notificationRepository.findFirstByNotificationId(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification introuvable : " + notificationId));
        return ResponseEntity.ok(n);
    }

    @GetMapping
    @Operation(
            summary = "Lister les notifications",
            description = "Retourne la liste paginée des notifications. Filtres optionnels : userId, status, sourceService"
    )
    @ApiResponse(responseCode = "200", description = "Liste retournée avec succès")
    public ResponseEntity<Page<Notification>> list(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sourceService,
            Pageable pageable) {
        return ResponseEntity.ok(notificationService.list(userId, status, sourceService, pageable));
    }

    @PostMapping("/{notificationId}/retry")
    @Operation(
            summary = "Relancer une notification en échec",
            description = "Relance manuellement une notification avec statut FAILED. Max 3 tentatives avec backoff exponentiel."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Relance acceptée"),
            @ApiResponse(responseCode = "400", description = "Retry non applicable - statut != FAILED"),
            @ApiResponse(responseCode = "404", description = "Notification introuvable")
    })
    public ResponseEntity<NotificationResponseDTO> retry(
            @PathVariable String notificationId) {
        return ResponseEntity.accepted().body(notificationService.retry(notificationId));
    }

    @GetMapping("/health")
    @Operation(
            summary = "Health-check",
            description = "Vérifie que le microservice G5 est opérationnel"
    )
    @ApiResponse(responseCode = "200", description = "Service UP")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(health);
    }

    private void validateChannelAndRecipient(NotificationRequestDTO dto) {
        if (dto.getRecipient() == null) {
            throw new IllegalArgumentException("recipient est obligatoire");
        }
        switch (dto.getChannel()) {
            case "EMAIL" -> {
                if (isBlank(dto.getRecipient().getEmail()))
                    throw new IllegalArgumentException("recipient.email obligatoire pour EMAIL");
            }
            case "SMS" -> {
                if (isBlank(dto.getRecipient().getPhone()))
                    throw new IllegalArgumentException("recipient.phone obligatoire pour SMS");
            }
            case "PUSH" -> {
                if (isBlank(dto.getRecipient().getDeviceToken()))
                    throw new IllegalArgumentException("recipient.deviceToken obligatoire pour PUSH");
            }
            case "LOG" -> {
                // Canal LOG (admin) : pas de destinataire obligatoire
                // userId peut être "system" ou un userId admin
                log.info("[G5] Canal LOG — notification admin vers fichier log");
            }
            default -> throw new IllegalArgumentException("Canal non supporté : " + dto.getChannel()
                    + ". Valeurs : EMAIL, SMS, PUSH, LOG");
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}