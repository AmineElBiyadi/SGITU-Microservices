package ma.sgitu.g5.mapper;

import ma.sgitu.g5.dto.request.NotificationRequestDTO;
import ma.sgitu.g5.dto.response.NotificationResponseDTO;
import ma.sgitu.g5.entity.Notification;
import ma.sgitu.g5.entity.NotificationStatus;
import ma.sgitu.g5.entity.NotificationType;
import org.mapstruct.*;

import java.time.LocalDateTime;

/**
 * NotificationMapper — Conversion automatique DTO ↔ Entité via MapStruct.
 * MapStruct génère l'implémentation à la compilation (zéro réflexion à runtime).
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface NotificationMapper {

    /**
     * Convertit un NotificationRequestDTO en entité Notification.
     * Les champs du RecipientDTO sont "aplatis" dans l'entité.
     */
    @Mappings({
        @Mapping(target = "id",              ignore = true),
        @Mapping(target = "userId",          source = "recipient.userId"),
        @Mapping(target = "email",           source = "recipient.email"),
        @Mapping(target = "phone",           source = "recipient.phone"),
        @Mapping(target = "deviceToken",     source = "recipient.deviceToken"),
        @Mapping(target = "recipient",       ignore = true),   // résolu manuellement
        @Mapping(target = "status",          constant = "PENDING"),
        @Mapping(target = "type",            ignore = true),   // converti via @AfterMapping
        @Mapping(target = "subject",         ignore = true),   // hydraté par TemplateService
        @Mapping(target = "content",         ignore = true),   // hydraté par TemplateService
        @Mapping(target = "provider",        ignore = true),
        @Mapping(target = "retryCount",      ignore = true),
        @Mapping(target = "createdAt",       ignore = true),
        @Mapping(target = "sentAt",          ignore = true)
    })
    Notification toEntity(NotificationRequestDTO dto);

    /**
     * Résout NotificationType à partir du channel string après le mapping principal.
     */
    @AfterMapping
    default void resolveType(@MappingTarget Notification entity, NotificationRequestDTO dto) {
        try {
            entity.setType(NotificationType.valueOf(dto.getChannel().toUpperCase()));
        } catch (Exception e) {
            entity.setType(NotificationType.EMAIL);
        }
        entity.setCreatedAt(LocalDateTime.now());
        if (entity.getStatus() == null) {
            entity.setStatus(NotificationStatus.PENDING);
        }
        if (entity.getPriority() == null) {
            entity.setPriority("NORMAL");
        }
        // Résolution du champ recipient (string simple)
        if (dto.getRecipient() != null) {
            String recipientStr = switch (dto.getChannel()) {
                case "EMAIL" -> dto.getRecipient().getEmail();
                case "SMS"   -> dto.getRecipient().getPhone();
                case "PUSH"  -> dto.getRecipient().getDeviceToken();
                default      -> "unknown";
            };
            entity.setRecipient(recipientStr != null ? recipientStr : "unresolved");
        }
    }

    /**
     * Convertit une entité Notification en NotificationResponseDTO.
     */
    @Mappings({
        @Mapping(target = "status",    expression = "java(notification.getStatus().name())"),
        @Mapping(target = "message",   constant = "Notification récupérée"),
        @Mapping(target = "queuedAt",  expression = "java(notification.getCreatedAt() != null ? notification.getCreatedAt().toString() : \"\")"),
        @Mapping(target = "channel",   source = "channel")
    })
    NotificationResponseDTO toResponseDTO(Notification notification);
}
