package com.g7suivivehicules.service;

import com.g7suivivehicules.entity.Alert.Severite;
import com.g7suivivehicules.entity.Alert.TypeAlert;
import com.g7suivivehicules.entity.PositionGPS;
import com.g7suivivehicules.entity.Telemetrie;
import com.g7suivivehicules.repository.ArretRepository;
import com.g7suivivehicules.repository.PositionGPSRepository;
import com.g7suivivehicules.repository.VehiculeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnomalyDetectionServiceTest {

    @Mock
    private AlertService alertService;

    @Mock
    private SeuilConfigService seuilConfigService;

    @Mock
    private ArretRepository arretRepository;

    @Mock
    private PositionGPSRepository positionGPSRepository;

    @Mock
    private G4IntegrationService g4IntegrationService;

    @Mock
    private VehiculeRepository vehiculeRepository;

    @InjectMocks
    private AnomalyDetectionService anomalyDetectionService;

    private UUID vehiculeId;
    private PositionGPS normalPosition;
    private PositionGPS speedingPosition;

    @BeforeEach
    void setUp() {
        vehiculeId = UUID.randomUUID();
        
        normalPosition = PositionGPS.builder()
                .id(UUID.randomUUID())
                .vehiculeId(vehiculeId)
                .latitude(48.8566)
                .longitude(2.3522)
                .vitesse(45.0) // En dessous du seuil de 50.0
                .timestamp(LocalDateTime.now())
                .build();

        speedingPosition = PositionGPS.builder()
                .id(UUID.randomUUID())
                .vehiculeId(vehiculeId)
                .latitude(48.8566)
                .longitude(2.3522)
                .vitesse(80.0) // Au-dessus du seuil de 50.0
                .timestamp(LocalDateTime.now())
                .build();

        // Mocks globaux pour éviter des NullPointer dans les autres détections
        lenient().when(seuilConfigService.getVitesseMax()).thenReturn(50.0);
        lenient().when(seuilConfigService.getFreinageDeceleration()).thenReturn(-3.0);
        lenient().when(seuilConfigService.getImmobilisationMin()).thenReturn(5);
        lenient().when(seuilConfigService.getNbPositions()).thenReturn(3);
        lenient().when(seuilConfigService.getRayonMetres()).thenReturn(50.0);
        lenient().when(positionGPSRepository.findByVehiculeIdOrderByTimestampDesc(any(), any()))
                .thenReturn(Collections.emptyList());
    }

    @Test
    void detecterAnomalies_WhenSpeedIsNormal_ShouldResolveAlert() {
        // Act
        anomalyDetectionService.detecterAnomalies(normalPosition, null);

        // Assert
        verify(alertService, times(1)).resoudreAutomatiquement(vehiculeId, TypeAlert.VITESSE_EXCESSIVE);
        verify(alertService, never()).creerOuMettreAJour(
                eq(vehiculeId), eq(TypeAlert.VITESSE_EXCESSIVE), any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    void detecterAnomalies_WhenSpeeding_ShouldCreateAlert() {
        // Act
        anomalyDetectionService.detecterAnomalies(speedingPosition, null);

        // Assert
        verify(alertService, times(1)).creerOuMettreAJour(
                eq(vehiculeId),
                eq(TypeAlert.VITESSE_EXCESSIVE),
                eq(48.8566),
                eq(2.3522),
                eq(80.0),
                eq(50.0),
                eq(Severite.CRITIQUE), // 80 >= 50 * 1.5 -> CRITIQUE
                anyString()
        );
        verify(alertService, never()).resoudreAutomatiquement(vehiculeId, TypeAlert.VITESSE_EXCESSIVE);
    }

    @Test
    void detecterAnomalies_WhenTemperatureIsHigh_ShouldCreateAlert() {
        // Arrange
        Telemetrie hotTelemetrie = Telemetrie.builder()
                .id(UUID.randomUUID())
                .vehiculeId(vehiculeId)
                .temperature(110.0) // Seuil 90.0
                .timestamp(LocalDateTime.now())
                .build();
        when(seuilConfigService.getTemperatureCritique()).thenReturn(90.0);

        // Act
        anomalyDetectionService.detecterAnomalies(normalPosition, hotTelemetrie);

        // Assert
        verify(alertService, times(1)).creerOuMettreAJour(
                eq(vehiculeId),
                eq(TypeAlert.TEMPERATURE_CRITIQUE),
                eq(48.8566),
                eq(2.3522),
                eq(110.0),
                eq(90.0),
                eq(Severite.CRITIQUE),
                anyString()
        );
    }

    @Test
    void detecterAnomalies_WhenTemperatureIsNormal_ShouldResolveAlert() {
        // Arrange
        Telemetrie normalTelemetrie = Telemetrie.builder()
                .id(UUID.randomUUID())
                .vehiculeId(vehiculeId)
                .temperature(75.0) // Seuil 90.0
                .timestamp(LocalDateTime.now())
                .build();
        when(seuilConfigService.getTemperatureCritique()).thenReturn(90.0);

        // Act
        anomalyDetectionService.detecterAnomalies(normalPosition, normalTelemetrie);

        // Assert
        verify(alertService, times(1)).resoudreAutomatiquement(vehiculeId, TypeAlert.TEMPERATURE_CRITIQUE);
    }

    @Test
    void detecterAnomalies_WhenFuelIsLow_ShouldCreateAlert() {
        // Arrange
        Telemetrie lowFuel = Telemetrie.builder()
                .id(UUID.randomUUID())
                .vehiculeId(vehiculeId)
                .carburant(4.0) // Seuil 10.0
                .timestamp(LocalDateTime.now())
                .build();
        when(seuilConfigService.getCarburantCritique()).thenReturn(10.0);

        // Act
        anomalyDetectionService.detecterAnomalies(normalPosition, lowFuel);

        // Assert
        verify(alertService, times(1)).creerOuMettreAJour(
                eq(vehiculeId),
                eq(TypeAlert.CARBURANT_CRITIQUE),
                eq(48.8566),
                eq(2.3522),
                eq(4.0),
                eq(10.0),
                eq(Severite.CRITIQUE),
                anyString()
        );
    }

    @Test
    void detecterAnomalies_WhenFuelIsNormal_ShouldResolveAlert() {
        // Arrange
        Telemetrie normalFuel = Telemetrie.builder()
                .id(UUID.randomUUID())
                .vehiculeId(vehiculeId)
                .carburant(60.0)
                .timestamp(LocalDateTime.now())
                .build();
        when(seuilConfigService.getCarburantCritique()).thenReturn(10.0);

        // Act
        anomalyDetectionService.detecterAnomalies(normalPosition, normalFuel);

        // Assert
        verify(alertService, times(1)).resoudreAutomatiquement(vehiculeId, TypeAlert.CARBURANT_CRITIQUE);
    }

    @Test
    void detecterAnomalies_WhenImmobilizedNearStop_ShouldNotCreateAlert() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        PositionGPS p1 = PositionGPS.builder().vitesse(0.0).timestamp(now).build();
        PositionGPS p2 = PositionGPS.builder().vitesse(0.0).timestamp(now.minusMinutes(3)).build();
        PositionGPS p3 = PositionGPS.builder().vitesse(0.0).timestamp(now.minusMinutes(6)).build();

        when(positionGPSRepository.findByVehiculeIdOrderByTimestampDesc(eq(vehiculeId), any()))
                .thenReturn(java.util.Arrays.asList(p1, p2, p3));
        
        // Simule un arrêt proche
        when(arretRepository.findArretsDansRayon(anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(java.util.Collections.singletonList(new com.g7suivivehicules.entity.Arret()));

        // Act
        anomalyDetectionService.detecterAnomalies(normalPosition, null);

        // Assert
        verify(alertService, times(1)).resoudreAutomatiquement(vehiculeId, TypeAlert.IMMOBILISATION);
    }

    @Test
    void detecterAnomalies_WhenImmobilizedFarFromStop_ShouldCreateAlert() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        PositionGPS p1 = PositionGPS.builder().vitesse(0.0).timestamp(now).build();
        PositionGPS p2 = PositionGPS.builder().vitesse(0.0).timestamp(now.minusMinutes(3)).build();
        PositionGPS p3 = PositionGPS.builder().vitesse(0.0).timestamp(now.minusMinutes(6)).build();

        when(positionGPSRepository.findByVehiculeIdOrderByTimestampDesc(eq(vehiculeId), any()))
                .thenReturn(java.util.Arrays.asList(p1, p2, p3));
        
        // Simule aucun arrêt proche
        when(arretRepository.findArretsDansRayon(anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(java.util.Collections.emptyList());

        // Act
        anomalyDetectionService.detecterAnomalies(normalPosition, null);

        // Assert
        verify(alertService, times(1)).creerOuMettreAJour(
                eq(vehiculeId),
                eq(TypeAlert.IMMOBILISATION),
                eq(48.8566),
                eq(2.3522),
                eq(5.0),
                eq(5.0),
                eq(Severite.MOYENNE),
                anyString()
        );
    }
}
