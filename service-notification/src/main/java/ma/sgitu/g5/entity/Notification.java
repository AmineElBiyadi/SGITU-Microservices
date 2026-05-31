package ma.sgitu.g5.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entité JPA pour le suivi des notifications.
 * Inclut des index pour optimiser les recherches par userId et statut.
 */
@Entity
@Table(name = "notifications",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_source_notif",
                        columnNames = {"source_service", "notification_id"}
                )
        },
        indexes = {
                @Index(name = "idx_source_notif", columnList = "source_service, notification_id"),
                @Index(name = "idx_user_status",   columnList = "user_id, status")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Clé de déduplication = (source_service + notification_id) — voir @UniqueConstraint
    @Column(name = "notification_id", nullable = false, length = 100)
    private String notificationId;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NotificationType type; // EMAIL, SMS, PUSH

    @Column(nullable = false, length = 10)
    private String channel;

    @Column(length = 255)
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = true, length = 255)
    private String recipient;

    @Column(length = 255)
    private String email;

    @Column(length = 50)
    private String phone;

    @Column(name = "device_token", length = 255)
    private String deviceToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NotificationStatus status;

    @Column(length = 100)
    private String provider;

    // Normalisé en majuscules à la réception — utilisé comme namespace de déduplication
    @Column(name = "source_service", nullable = false, length = 100)
    private String sourceService;

    @Column(name = "event_type", length = 100)
    private String eventType;

    @Column(length = 20)
    private String priority;

    @Column(name = "retry_count")
    @Builder.Default
    private int retryCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null)
            this.status = NotificationStatus.PENDING;
        if (this.priority == null)
            this.priority = "NORMAL";
    }
}
