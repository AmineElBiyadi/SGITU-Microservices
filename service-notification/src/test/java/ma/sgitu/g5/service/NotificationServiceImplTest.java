package ma.sgitu.g5.service;

import ma.sgitu.g5.dto.request.MetadataDTO;
import ma.sgitu.g5.dto.request.NotificationRequestDTO;
import ma.sgitu.g5.dto.request.RecipientDTO;
import ma.sgitu.g5.dto.response.NotificationResponseDTO;
import ma.sgitu.g5.dto.response.SendResultDTO;
import ma.sgitu.g5.entity.Notification;
import ma.sgitu.g5.entity.NotificationStatus;
import ma.sgitu.g5.mapper.NotificationMapper;
import ma.sgitu.g5.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires — NotificationServiceImpl")
class NotificationServiceImplTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private ITemplateService       templateService;
    @Mock private IChannelRouter         channelRouter;
    @Mock private IRetryService          retryService;
    @Mock private NotificationMapper     notificationMapper;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private NotificationRequestDTO validEmailDto;
    private Notification           savedEntity;

    @BeforeEach
    void setUp() {
        RecipientDTO recipient = new RecipientDTO();
        recipient.setUserId("user-123");
        recipient.setEmail("test@sgitu.ma");

        validEmailDto = new NotificationRequestDTO();
        validEmailDto.setNotificationId("notif-uuid-001");
        validEmailDto.setSourceService("G1_BILLETTERIE");
        validEmailDto.setEventType("TICKET_CREATED");
        validEmailDto.setChannel("EMAIL");
        validEmailDto.setPriority("NORMAL");
        validEmailDto.setRecipient(recipient);

        MetadataDTO meta = new MetadataDTO();
        validEmailDto.setMetadata(meta);

        savedEntity = Notification.builder()
                .notificationId("notif-uuid-001")
                .sourceService("G1_BILLETTERIE")
                .channel("EMAIL")
                .status(NotificationStatus.PENDING)
                .userId("user-123")
                .email("test@sgitu.ma")
                .eventType("TICKET_CREATED")
                .priority("NORMAL")
                .build();

        SendResultDTO ok = new SendResultDTO();
        ok.setSuccess(true);
        ok.setProvider("MOCK");
        lenient().when(channelRouter.route(any(), anyString(), anyString())).thenReturn(ok);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Tests : send()
    // ────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("send() → nouvelle notification → retourne QUEUED")
    void send_nouvelleNotification_retourneQueued() {
        when(notificationRepository.findBySourceServiceAndNotificationId(anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(templateService.hydrateMessage(anyString(), any())).thenReturn("Message test");
        when(templateService.hydrateSubject(anyString(), any())).thenReturn("Sujet test");
        when(notificationMapper.toEntity(any())).thenReturn(savedEntity);
        when(notificationRepository.save(any())).thenReturn(savedEntity);

        NotificationResponseDTO response = notificationService.send(validEmailDto);

        assertThat(response.getStatus()).isEqualTo("QUEUED");
        assertThat(response.getNotificationId()).isEqualTo("notif-uuid-001");
        assertThat(response.getChannel()).isEqualTo("EMAIL");
        verify(notificationRepository, atLeastOnce()).save(any());
    }

    @Test
    @DisplayName("send() → doublon détecté → retourne ALREADY_QUEUED")
    void send_doublonDetecte_retourneAlreadyQueued() {
        when(notificationRepository.findBySourceServiceAndNotificationId("G1_BILLETTERIE", "notif-uuid-001"))
                .thenReturn(Optional.of(savedEntity));

        NotificationResponseDTO response = notificationService.send(validEmailDto);

        assertThat(response.getStatus()).isEqualTo("ALREADY_QUEUED");
        assertThat(response.getOriginalSourceService()).isEqualTo("G1_BILLETTERIE");
        // On ne doit pas sauvegarder une deuxième fois
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("send() → notificationId null → UUID généré automatiquement")
    void send_notificationIdNull_uuidGenere() {
        validEmailDto.setNotificationId(null);
        when(notificationRepository.findBySourceServiceAndNotificationId(anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(templateService.hydrateMessage(anyString(), any())).thenReturn("msg");
        when(templateService.hydrateSubject(anyString(), any())).thenReturn("subj");
        when(notificationMapper.toEntity(any())).thenReturn(savedEntity);
        when(notificationRepository.save(any())).thenReturn(savedEntity);

        NotificationResponseDTO response = notificationService.send(validEmailDto);

        assertThat(response.getStatus()).isEqualTo("QUEUED");
    }

    @Test
    @DisplayName("send() → sourceService normalisé en majuscules")
    void send_sourceServiceNormalise() {
        validEmailDto.setSourceService("g1_billetterie"); // minuscules
        when(notificationRepository.findBySourceServiceAndNotificationId(anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(templateService.hydrateMessage(anyString(), any())).thenReturn("msg");
        when(templateService.hydrateSubject(anyString(), any())).thenReturn("subj");
        when(notificationMapper.toEntity(any())).thenReturn(savedEntity);
        when(notificationRepository.save(any())).thenReturn(savedEntity);

        notificationService.send(validEmailDto);

        assertThat(validEmailDto.getSourceService()).isEqualTo("G1_BILLETTERIE");
    }

    // ────────────────────────────────────────────────────────────────────────
    // Tests : retry()
    // ────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("retry() → notification FAILED → retourne QUEUED et incrémente retryCount")
    void retry_notificationFailed_retourneQueued() {
        savedEntity.setStatus(NotificationStatus.FAILED);
        savedEntity.setRetryCount(1);

        when(notificationRepository.findFirstByNotificationId("notif-uuid-001"))
                .thenReturn(Optional.of(savedEntity));
        when(notificationRepository.save(any())).thenReturn(savedEntity);

        NotificationResponseDTO response = notificationService.retry("notif-uuid-001");

        assertThat(response.getStatus()).isEqualTo("QUEUED");
        assertThat(savedEntity.getRetryCount()).isEqualTo(2);
        verify(notificationRepository, atLeastOnce()).save(savedEntity);
    }

    @Test
    @DisplayName("retry() → notification SENT (pas FAILED) → retourne message non applicable")
    void retry_notificationNonFailed_retourneNonApplicable() {
        savedEntity.setStatus(NotificationStatus.SENT);

        when(notificationRepository.findFirstByNotificationId("notif-uuid-001"))
                .thenReturn(Optional.of(savedEntity));

        NotificationResponseDTO response = notificationService.retry("notif-uuid-001");

        assertThat(response.getStatus()).isEqualTo("SENT");
        assertThat(response.getMessage()).contains("Retry non applicable");
    }

    @Test
    @DisplayName("retry() → notification introuvable → IllegalArgumentException")
    void retry_notificationIntrouvable_throwsException() {
        when(notificationRepository.findFirstByNotificationId("id-inexistant"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.retry("id-inexistant"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Notification introuvable");
    }

    // ────────────────────────────────────────────────────────────────────────
    // Tests : updateStatus()
    // ────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateStatus() → résultat succès → statut SENT")
    void updateStatus_succes_statutSent() {
        SendResultDTO success = new SendResultDTO();
        success.setSuccess(true);
        success.setProvider("SMTP");

        notificationService.updateStatus(savedEntity, success);

        assertThat(savedEntity.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(savedEntity.getProvider()).isEqualTo("SMTP");
        verify(notificationRepository).save(savedEntity);
    }

    @Test
    @DisplayName("updateStatus() → résultat échec → handleFailure appelé")
    void updateStatus_echec_handleFailureAppele() {
        SendResultDTO failure = new SendResultDTO();
        failure.setSuccess(false);
        failure.setErrorCode("SMTP_ERROR");
        when(retryService.shouldRetry(anyInt())).thenReturn(false);

        notificationService.updateStatus(savedEntity, failure);

        assertThat(savedEntity.getStatus()).isEqualTo(NotificationStatus.FAILED);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Tests : handleFailure()
    // ────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("handleFailure() → retryCount < 3 → statut reste PENDING et scheduleRetry appelé")
    void handleFailure_avecRetryPossible_resteEnPending() {
        savedEntity.setRetryCount(1);
        when(retryService.shouldRetry(1)).thenReturn(true);
        when(retryService.nextDelaySeconds(1)).thenReturn(60);

        notificationService.handleFailure(savedEntity, "SMTP_TIMEOUT");

        assertThat(savedEntity.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(savedEntity.getRetryCount()).isEqualTo(2);
        verify(retryService).scheduleRetry(anyString(), eq(60));
    }

    @Test
    @DisplayName("handleFailure() → retryCount >= 3 → statut FAILED définitif")
    void handleFailure_maxRetryAtteint_passeEnFailed() {
        savedEntity.setRetryCount(3);
        when(retryService.shouldRetry(3)).thenReturn(false);

        notificationService.handleFailure(savedEntity, "SMTP_TIMEOUT");

        assertThat(savedEntity.getStatus()).isEqualTo(NotificationStatus.FAILED);
        verify(retryService, never()).scheduleRetry(anyString(), anyInt());
    }
}
