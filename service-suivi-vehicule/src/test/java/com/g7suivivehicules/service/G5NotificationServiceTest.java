package com.g7suivivehicules.service;

import com.g7suivivehicules.entity.Alert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class G5NotificationServiceTest {

    @Autowired
    private G5NotificationService g5NotificationService;

    @MockBean
    private RestTemplate restTemplate;

    @Test
    void notifierConducteur_WhenServiceFails_ShouldTriggerFallback() throws Exception {
        // Arrange
        Alert alert = Alert.builder()
                .id(UUID.randomUUID())
                .vehiculeId(UUID.randomUUID())
                .typeAlert(Alert.TypeAlert.VITESSE_EXCESSIVE)
                .latitude(48.8566)
                .longitude(2.3522)
                .valeur(80.0)
                .seuil(50.0)
                .severite(Alert.Severite.CRITIQUE)
                .statut(Alert.StatutAlert.OUVERTE)
                .message("Vitesse excessive")
                .timestampDebut(LocalDateTime.now())
                .build();

        // On fait échouer le RestTemplate
        when(restTemplate.postForEntity(any(String.class), any(), eq(String.class)))
                .thenThrow(new ResourceAccessException("Connection timeout"));

        // Act
        CompletableFuture<Void> result = g5NotificationService.notifierConducteur(alert);

        // Assert
        assertNotNull(result);
        // Le fallback de resilience4j retourne un CompletableFuture complété avec null
        assertTrue(result.isDone());
        // Vérifie qu'on a bien tenté l'appel via RestTemplate
        verify(restTemplate, atLeastOnce()).postForEntity(any(String.class), any(), eq(String.class));
    }
}
