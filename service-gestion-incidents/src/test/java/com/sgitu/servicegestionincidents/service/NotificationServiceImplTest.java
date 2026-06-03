package com.sgitu.servicegestionincidents.service;

import com.sgitu.servicegestionincidents.dto.response.UtilisateurDTO;
import com.sgitu.servicegestionincidents.messaging.event.NotificationEvent;
import com.sgitu.servicegestionincidents.messaging.producer.NotificationProducer;
import com.sgitu.servicegestionincidents.model.entity.Incident;
import com.sgitu.servicegestionincidents.model.enums.NiveauGravite;
import com.sgitu.servicegestionincidents.model.enums.StatutIncident;
import com.sgitu.servicegestionincidents.model.enums.TypeIncident;
import com.sgitu.servicegestionincidents.service.utilisateur.UtilisateurService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationProducer notificationProducer;

    @Mock
    private UtilisateurService utilisateurService;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notificationService, "fallbackEmail", "test-fallback@sgitu.ma");
        ReflectionTestUtils.setField(notificationService, "fallbackPhone", "+212111111111");
    }

    @Test
    void envoyerConfirmation_usesFallbackEmailWhenG3Unavailable() {
        when(utilisateurService.findById(42L)).thenReturn(CompletableFuture.completedFuture(null));

        Incident incident = Incident.builder()
                .id(1L)
                .reference("INC-001")
                .type(TypeIncident.PANNE_VEHICULE)
                .statut(StatutIncident.NOUVEAU)
                .gravite(NiveauGravite.MOYEN)
                .declarantId(42L)
                .dateSignalement(LocalDateTime.now())
                .build();

        notificationService.envoyerConfirmation(incident);

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationProducer).envoyerNotification(captor.capture());

        NotificationEvent event = captor.getValue();
        assertThat(event.getChannel()).isEqualTo("EMAIL");
        assertThat(event.getRecipient().getUserId()).isEqualTo("42");
        assertThat(event.getRecipient().getEmail()).isEqualTo("test-fallback@sgitu.ma");
    }

    @Test
    void envoyerAssignation_usesFallbackPhoneForSmsWhenG3Unavailable() {
        when(utilisateurService.findById(7L)).thenReturn(CompletableFuture.completedFuture(null));

        Incident incident = Incident.builder()
                .id(2L)
                .reference("INC-002")
                .type(TypeIncident.PANNE_VEHICULE)
                .statut(StatutIncident.ASSIGNE)
                .gravite(NiveauGravite.ELEVE)
                .responsableId(7L)
                .declarantId(1L)
                .dateSignalement(LocalDateTime.now())
                .localisation(com.sgitu.servicegestionincidents.model.entity.Localisation.builder()
                        .latitude(33.5)
                        .longitude(-7.6)
                        .build())
                .build();

        notificationService.envoyerAssignation(incident);

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationProducer, org.mockito.Mockito.times(2)).envoyerNotification(captor.capture());

        NotificationEvent smsEvent = captor.getAllValues().stream()
                .filter(e -> "SMS".equals(e.getChannel()))
                .findFirst()
                .orElseThrow();
        assertThat(smsEvent.getRecipient().getPhone()).isEqualTo("+212111111111");
    }
}
