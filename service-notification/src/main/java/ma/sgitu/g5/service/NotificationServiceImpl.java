package ma.sgitu.g5.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.sgitu.g5.dto.request.NotificationRequestDTO;
import ma.sgitu.g5.dto.response.NotificationResponseDTO;
import ma.sgitu.g5.dto.response.SendResultDTO;
import ma.sgitu.g5.entity.Notification;
import ma.sgitu.g5.entity.NotificationStatus;
import ma.sgitu.g5.mapper.NotificationMapper;
import ma.sgitu.g5.repository.NotificationRepository;
import ma.sgitu.g5.repository.specification.NotificationSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements INotificationService {

    private final NotificationRepository notificationRepository;
    private final ITemplateService templateService;
    private final IChannelRouter channelRouter;
    private final IRetryService retryService;
    private final NotificationMapper notificationMapper;

    // ─────────────────────────────────────────────────────────────────────────
    // SEND — Point d'entrée principal
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public NotificationResponseDTO send(NotificationRequestDTO dto) {
        String notificationId = dto.getNotificationId();
        if (notificationId == null || notificationId.isBlank()) {
            notificationId = UUID.randomUUID().toString();
        }

        // Normalisation sourceService (évite les collisions G1 vs g1 vs G1_BILLETTERIE)
        String sourceService = (dto.getSourceService() != null)
                ? dto.getSourceService().toUpperCase().trim()
                : "UNKNOWN";
        dto.setSourceService(sourceService);

        // ── IDEMPOTENCE : clé composite (sourceService + notificationId) ──────
        // Si la notification a déjà été prise en charge, on retourne ses données
        // originales dans le corps de la réponse 202 (exigence prof).
        final String finalNotifId = notificationId;
        java.util.Optional<Notification> existing =
                notificationRepository.findBySourceServiceAndNotificationId(sourceService, finalNotifId);

        if (existing.isPresent()) {
            Notification original = existing.get();
            log.warn("[G5] Doublon détecté (id={}  source={}) — ALREADY_QUEUED", notificationId, sourceService);
            return buildAlreadyQueuedResponse(original);
        }

        String message = templateService.hydrateMessage(dto.getEventType(), dto.getMetadata());
        String subject  = templateService.hydrateSubject(dto.getEventType(), dto.getMetadata());

        // ── MapStruct : conversion automatique DTO → Entité ──────────────────
        Notification entity = notificationMapper.toEntity(dto);
        entity.setNotificationId(notificationId);
        entity.setSubject(subject);
        entity.setContent(message);
        notificationRepository.save(entity);

        dispatchAsync(entity, dto, subject, message);

        log.info("[G5] Notification QUEUED : {} | channel={}", notificationId, dto.getChannel());
        return buildQueuedResponse(entity);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LIST — Recherche paginée avec filtres dynamiques
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public Page<Notification> list(String userId, String status, String sourceService, Pageable pageable) {
        NotificationStatus enumStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                enumStatus = NotificationStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException(
                        "Statut invalide : " + status + ". Valeurs acceptées : PENDING, SENT, FAILED");
            }
        }
        Specification<Notification> spec =
                NotificationSpecification.withFilters(userId, enumStatus, sourceService, null, null, null);
        return notificationRepository.findAll(spec, pageable);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RETRY — Relance manuelle d'une notification FAILED
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public NotificationResponseDTO retry(String notificationId) {
        Notification entity = notificationRepository.findFirstByNotificationId(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification introuvable : " + notificationId));

        if (entity.getStatus() != NotificationStatus.FAILED) {
            return buildSimpleResponse(notificationId, entity.getStatus().name(),
                    "Retry non applicable sur ce statut", entity.getChannel());
        }

        entity.setStatus(NotificationStatus.PENDING);
        entity.setRetryCount(entity.getRetryCount() + 1);
        notificationRepository.save(entity);

        dispatchAsyncFromEntity(entity);
        return buildSimpleResponse(notificationId, "QUEUED", "Relance en cours", entity.getChannel());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DISPATCH — Envoi asynchrone
    // ─────────────────────────────────────────────────────────────────────────
    @Async
    public void dispatchAsync(Notification entity, NotificationRequestDTO dto, String subject, String message) {
        try {
            SendResultDTO result = channelRouter.route(dto, subject, message);
            updateStatus(entity, result);
        } catch (Exception ex) {
            handleFailure(entity, ex.getMessage());
        }
    }

    @Async
    public void dispatchAsyncFromEntity(Notification entity) {
        try {
            NotificationRequestDTO dto = rebuildDtoFromEntity(entity);
            SendResultDTO result = channelRouter.route(dto, entity.getSubject(), entity.getContent());
            updateStatus(entity, result);
        } catch (Exception ex) {
            handleFailure(entity, ex.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public void updateStatus(Notification entity, SendResultDTO result) {
        if (result.isSuccess()) {
            entity.setStatus(NotificationStatus.SENT);
            entity.setSentAt(LocalDateTime.now());
            entity.setProvider(result.getProvider());
        } else {
            handleFailure(entity, result.getErrorCode());
        }
        notificationRepository.save(entity);
    }

    @Transactional
    public void handleFailure(Notification entity, String errorReason) {
        if (retryService.shouldRetry(entity.getRetryCount())) {
            int delay = retryService.nextDelaySeconds(entity.getRetryCount());
            entity.setStatus(NotificationStatus.PENDING);
            entity.setRetryCount(entity.getRetryCount() + 1);
            notificationRepository.save(entity);
            // Relance différée via TaskScheduler (backoff exponentiel)
            retryService.scheduleRetry(entity.getNotificationId(), delay);
        } else {
            entity.setStatus(NotificationStatus.FAILED);
            notificationRepository.save(entity);
            log.error("[G5] Notification FAILED définitivement : {} | Raison : {}",
                    entity.getNotificationId(), errorReason);
        }
    }

    private NotificationRequestDTO rebuildDtoFromEntity(Notification entity) {
        NotificationRequestDTO dto = new NotificationRequestDTO();
        dto.setNotificationId(entity.getNotificationId());
        dto.setSourceService(entity.getSourceService());
        dto.setEventType(entity.getEventType());
        dto.setChannel(entity.getChannel());
        dto.setPriority(entity.getPriority());
        ma.sgitu.g5.dto.request.RecipientDTO r = new ma.sgitu.g5.dto.request.RecipientDTO();
        r.setUserId(entity.getUserId());
        r.setEmail(entity.getEmail());
        r.setPhone(entity.getPhone());
        r.setDeviceToken(entity.getDeviceToken());
        dto.setRecipient(r);
        return dto;
    }

    /**
     * Réponse pour le cas ALREADY_QUEUED — contient les données de la notification originale.
     * Requis par la remarque du prof : "le corps doit contenir l'identifiant de la notif originale".
     */
    private NotificationResponseDTO buildAlreadyQueuedResponse(Notification original) {
        NotificationResponseDTO r = new NotificationResponseDTO();
        r.setNotificationId(original.getNotificationId());
        r.setStatus("ALREADY_QUEUED");
        r.setMessage("Notification déjà prise en charge — données originales retournées");
        r.setChannel(original.getChannel());
        r.setQueuedAt(original.getCreatedAt() != null ? original.getCreatedAt().toString() : null);
        // ── Données originales (idempotence enrichie) ────────────────────────
        r.setOriginalCreatedAt(original.getCreatedAt() != null ? original.getCreatedAt().toString() : null);
        r.setCurrentStatus(original.getStatus() != null ? original.getStatus().name() : null);
        r.setOriginalSourceService(original.getSourceService());
        return r;
    }

    /** Réponse pour une nouvelle notification acceptée (QUEUED). */
    private NotificationResponseDTO buildQueuedResponse(Notification entity) {
        NotificationResponseDTO r = new NotificationResponseDTO();
        r.setNotificationId(entity.getNotificationId());
        r.setStatus("QUEUED");
        r.setMessage("Notification prise en charge");
        r.setChannel(entity.getChannel());
        r.setQueuedAt(LocalDateTime.now().toString());
        return r;
    }

    /** Réponse simple (retry, statut non-FAILED, etc.). */
    private NotificationResponseDTO buildSimpleResponse(String id, String status, String msg, String ch) {
        NotificationResponseDTO r = new NotificationResponseDTO();
        r.setNotificationId(id);
        r.setStatus(status);
        r.setMessage(msg);
        r.setChannel(ch);
        r.setQueuedAt(LocalDateTime.now().toString());
        return r;
    }
}