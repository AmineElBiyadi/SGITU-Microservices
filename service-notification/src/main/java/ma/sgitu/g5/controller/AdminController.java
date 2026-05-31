package ma.sgitu.g5.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.sgitu.g5.entity.Notification;
import ma.sgitu.g5.entity.NotificationStatus;
import ma.sgitu.g5.repository.NotificationRepository;
import ma.sgitu.g5.repository.specification.NotificationSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AdminController — Endpoints d'administration pour le service de notification.
 *
 * Accès : ROLE_ADMIN requis sur tous les endpoints.
 * Toutes les opérations sont loggées dans logs/admin-operations.log (logback-admin.xml).
 *
 * NOTE PROF : "Au niveau admin, les notifications peuvent être des fichiers logs."
 * → Les opérations admin génèrent des entrées dans admin-operations.log (rotation quotidienne).
 * → Les notifications de type LOG sont routées vers LogFileAdapter (canal LOG).
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "API d'administration — Nécessite ROLE_ADMIN. Logs dans admin-operations.log")
public class AdminController {

    private final NotificationRepository notificationRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // STATISTIQUES
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/stats")
    @Operation(
            summary = "Statistiques globales",
            description = "Retourne les statistiques de notifications par statut et par canal"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statistiques retournées avec succès"),
            @ApiResponse(responseCode = "403", description = "Accès refusé - ROLE_ADMIN requis")
    })
    public ResponseEntity<Map<String, Object>> getStats() {
        log.info("[ADMIN] Récupération des statistiques globales");

        Map<String, Object> stats = new LinkedHashMap<>();

        Map<String, Long> statusStats = new HashMap<>();
        statusStats.put("PENDING", notificationRepository.countByStatus(NotificationStatus.PENDING));
        statusStats.put("SENT",    notificationRepository.countByStatus(NotificationStatus.SENT));
        statusStats.put("FAILED",  notificationRepository.countByStatus(NotificationStatus.FAILED));
        stats.put("byStatus", statusStats);

        Map<String, Long> channelStats = new HashMap<>();
        channelStats.put("EMAIL", notificationRepository.countByChannel("EMAIL"));
        channelStats.put("SMS",   notificationRepository.countByChannel("SMS"));
        channelStats.put("PUSH",  notificationRepository.countByChannel("PUSH"));
        channelStats.put("LOG",   notificationRepository.countByChannel("LOG"));
        stats.put("byChannel", channelStats);

        stats.put("total",     notificationRepository.count());
        stats.put("timestamp", LocalDateTime.now().toString());

        log.info("[ADMIN] Statistiques récupérées : {}", stats);
        return ResponseEntity.ok(stats);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LISTE DES NOTIFICATIONS — FILTRES DYNAMIQUES COMPLETS
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/notifications")
    @Operation(
            summary = "Lister toutes les notifications (admin)",
            description = "Retourne la liste paginée de toutes les notifications. "
                    + "Tous les filtres sont optionnels et combinables : "
                    + "status, channel, sourceService, userId, startDate, endDate."
    )
    @ApiResponse(responseCode = "200", description = "Liste retournée avec succès")
    public ResponseEntity<Page<Notification>> listAllNotifications(
            @Parameter(description = "Filtre sur le statut : PENDING, SENT, FAILED")
            @RequestParam(required = false) String status,

            @Parameter(description = "Filtre sur le canal : EMAIL, SMS, PUSH, LOG")
            @RequestParam(required = false) String channel,

            @Parameter(description = "Filtre sur le service source : G1_BILLETTERIE, G3_UTILISATEUR...")
            @RequestParam(required = false) String sourceService,

            @Parameter(description = "Filtre sur l'identifiant utilisateur")
            @RequestParam(required = false) String userId,

            @Parameter(description = "Date de début (ISO 8601) : 2026-01-01T00:00:00")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,

            @Parameter(description = "Date de fin (ISO 8601) : 2026-12-31T23:59:59")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,

            Pageable pageable) {

        log.info("[ADMIN] Liste notifications — status={}, channel={}, source={}, userId={}, start={}, end={}",
                status, channel, sourceService, userId, startDate, endDate);

        // ✅ CORRECTION : tous les filtres passent par la Specification (dates incluses)
        NotificationStatus enumStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                enumStatus = NotificationStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                return ResponseEntity.badRequest().build();
            }
        }

        Specification<Notification> spec = NotificationSpecification.withFilters(
                userId, enumStatus, sourceService, channel, startDate, endDate);

        Page<Notification> notifications = notificationRepository.findAll(spec, pageable);
        log.info("[ADMIN] {} notifications trouvées", notifications.getTotalElements());
        return ResponseEntity.ok(notifications);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RACCOURCIS DE FILTRAGE
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/notifications/failed")
    @Operation(summary = "Lister les notifications en échec",
               description = "Retourne uniquement les notifications avec statut FAILED")
    @ApiResponse(responseCode = "200", description = "Liste des échecs retournée avec succès")
    public ResponseEntity<Page<Notification>> listFailedNotifications(Pageable pageable) {
        log.info("[ADMIN] Récupération des notifications FAILED");
        Specification<Notification> spec = NotificationSpecification.withFilters(
                null, NotificationStatus.FAILED, null, null, null, null);
        Page<Notification> failed = notificationRepository.findAll(spec, pageable);
        log.info("[ADMIN] {} notifications FAILED trouvées", failed.getTotalElements());
        return ResponseEntity.ok(failed);
    }

    @GetMapping("/notifications/by-source/{sourceService}")
    @Operation(summary = "Lister les notifications par service source",
               description = "Retourne les notifications provenant d'un groupe (G1-G10)")
    @ApiResponse(responseCode = "200", description = "Liste retournée avec succès")
    public ResponseEntity<Page<Notification>> listBySourceService(
            @PathVariable String sourceService,
            Pageable pageable) {
        log.info("[ADMIN] Notifications pour le service source : {}", sourceService);
        Specification<Notification> spec = NotificationSpecification.withFilters(
                null, null, sourceService, null, null, null);
        Page<Notification> notifications = notificationRepository.findAll(spec, pageable);
        log.info("[ADMIN] {} notifications trouvées pour {}", notifications.getTotalElements(), sourceService);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/notifications/logs")
    @Operation(summary = "Lister les notifications de type LOG (admin)",
               description = "Retourne les notifications de canal LOG — fichiers logs générés au niveau admin")
    @ApiResponse(responseCode = "200", description = "Logs admin retournés avec succès")
    public ResponseEntity<Page<Notification>> listLogNotifications(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Pageable pageable) {
        log.info("[ADMIN] Récupération des notifications LOG — start={}, end={}", startDate, endDate);
        Specification<Notification> spec = NotificationSpecification.withFilters(
                null, null, null, "LOG", startDate, endDate);
        Page<Notification> logs = notificationRepository.findAll(spec, pageable);
        log.info("[ADMIN] {} entrées LOG trouvées", logs.getTotalElements());
        return ResponseEntity.ok(logs);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ACTIONS ADMIN
    // ─────────────────────────────────────────────────────────────────────────

    @DeleteMapping("/notifications/{id}")
    @Operation(summary = "Supprimer une notification (admin)",
               description = "Supprime définitivement une notification par son ID interne")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification supprimée"),
            @ApiResponse(responseCode = "404", description = "Notification introuvable")
    })
    public ResponseEntity<Map<String, String>> deleteNotification(@PathVariable Long id) {
        log.warn("[ADMIN] Suppression de la notification ID: {}", id);
        if (!notificationRepository.existsById(id)) {
            log.error("[ADMIN] Notification ID {} introuvable", id);
            return ResponseEntity.notFound().build();
        }
        notificationRepository.deleteById(id);
        log.info("[ADMIN] Notification ID {} supprimée avec succès", id);
        Map<String, String> response = new HashMap<>();
        response.put("message",   "Notification supprimée avec succès");
        response.put("id",        id.toString());
        response.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/notifications/{id}/force-retry")
    @Operation(summary = "Forcer la relance (admin)",
               description = "Force la relance d'une notification quel que soit son statut, réinitialise le compteur")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Relance forcée avec succès"),
            @ApiResponse(responseCode = "404", description = "Notification introuvable")
    })
    public ResponseEntity<Map<String, String>> forceRetry(@PathVariable Long id) {
        log.warn("[ADMIN] Force retry pour la notification ID: {}", id);
        Notification notification = notificationRepository.findById(id).orElse(null);
        if (notification == null) {
            log.error("[ADMIN] Notification ID {} introuvable", id);
            return ResponseEntity.notFound().build();
        }
        notification.setStatus(NotificationStatus.PENDING);
        notification.setRetryCount(0);
        notificationRepository.save(notification);
        log.info("[ADMIN] Force retry effectué pour notification ID: {}", id);
        Map<String, String> response = new HashMap<>();
        response.put("message",        "Relance forcée avec succès");
        response.put("notificationId", notification.getNotificationId());
        response.put("timestamp",      LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health/detailed")
    @Operation(summary = "Health check détaillé (admin)",
               description = "Retourne l'état détaillé du service avec métriques")
    @ApiResponse(responseCode = "200", description = "État détaillé retourné avec succès")
    public ResponseEntity<Map<String, Object>> detailedHealth() {
        log.info("[ADMIN] Health check détaillé demandé");
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status",    "UP");
        health.put("timestamp", LocalDateTime.now().toString());
        health.put("service",   "notification-service");
        health.put("version",   "1.0.0");
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalNotifications",   notificationRepository.count());
        metrics.put("pendingNotifications", notificationRepository.countByStatus(NotificationStatus.PENDING));
        metrics.put("failedNotifications",  notificationRepository.countByStatus(NotificationStatus.FAILED));
        metrics.put("logNotifications",     notificationRepository.countByChannel("LOG"));
        health.put("metrics", metrics);
        log.info("[ADMIN] Health check détaillé : {}", health);
        return ResponseEntity.ok(health);
    }
}
