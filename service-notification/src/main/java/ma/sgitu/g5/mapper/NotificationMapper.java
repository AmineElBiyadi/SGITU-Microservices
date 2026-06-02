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
        @Mapping(target = "recipient",       expression = "java(ma.sgitu.g5.mapper.NotificationMapper.resolveRecipientString(dto))"),
        @Mapping(target = "status",          constant = "PENDING"),
        @Mapping(target = "type",            expression = "java(ma.sgitu.g5.entity.NotificationType.valueOf(dto.getChannel().toUpperCase()))"),
        @Mapping(target = "subject",         ignore = true),
        @Mapping(target = "content",         ignore = true),
        @Mapping(target = "provider",        ignore = true),
        @Mapping(target = "retryCount",      ignore = true),
        @Mapping(target = "createdAt",       expression = "java(java.time.LocalDateTime.now())"),
        @Mapping(target = "sentAt",          ignore = true)
    })
    Notification toEntity(NotificationRequestDTO dto);

    /**
     * Résout la valeur du champ recipient (string) selon le canal.
     */
    static String resolveRecipientString(NotificationRequestDTO dto) {
        if (dto == null || dto.getRecipient() == null) {
            return "system";
        }
        return switch (dto.getChannel() != null ? dto.getChannel() : "") {
            case "EMAIL" -> dto.getRecipient().getEmail() != null ? dto.getRecipient().getEmail() : "unknown";
            case "SMS"   -> dto.getRecipient().getPhone() != null ? dto.getRecipient().getPhone() : "unknown";
            case "PUSH"  -> dto.getRecipient().getDeviceToken() != null ? dto.getRecipient().getDeviceToken() : "unknown";
            case "LOG"   -> dto.getRecipient().getUserId() != null ? dto.getRecipient().getUserId() : "system";
            default      -> "unknown";
        };
    }

    /**
     * @deprecated Conservé pour compatibilité ; la logique est dans {@link #resolveRecipientString}.
     */
    @Deprecated
    @AfterMapping
    default void resolveType(@MappingTarget Notification entity, NotificationRequestDTO dto) {
        if (entity.getType() == null && dto.getChannel() != null) {
            entity.setType(NotificationType.valueOf(dto.getChannel().toUpperCase()));
        }
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }
        if (entity.getStatus() == null) {
            entity.setStatus(NotificationStatus.PENDING);
        }
        if (entity.getPriority() == null) {
            entity.setPriority("NORMAL");
        }
        if (entity.getRecipient() == null) {
            entity.setRecipient(resolveRecipientString(dto));
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
