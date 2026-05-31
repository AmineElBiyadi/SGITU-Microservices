package com.g7suivivehicules.service;

import com.g7suivivehicules.dto.VehiculeRequest;
import com.g7suivivehicules.dto.VehiculeResponse;
import com.g7suivivehicules.entity.Vehicule;
import com.g7suivivehicules.exception.VehiculeNotFoundException;
import com.g7suivivehicules.repository.VehiculeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehiculeServiceTest {

    @Mock
    private VehiculeRepository vehiculeRepository;

    @InjectMocks
    private VehiculeService vehiculeService;

    private UUID vehiculeId;
    private Vehicule vehicule;
    private VehiculeRequest request;

    @BeforeEach
    void setUp() {
        vehiculeId = UUID.randomUUID();
        
        vehicule = Vehicule.builder()
                .id(vehiculeId)
                .immatriculation("BUS-G7-001")
                .type(Vehicule.TypeVehicule.BUS)
                .ligne("G4")
                .statut(Vehicule.StatutVehicule.DISPONIBLE)
                .conducteurId(UUID.randomUUID())
                .build();

        request = VehiculeRequest.builder()
                .immatriculation("BUS-G7-001")
                .type(Vehicule.TypeVehicule.BUS)
                .ligne("G4")
                .conducteurId(vehicule.getConducteurId())
                .build();
    }

    @Test
    void createVehicule_ShouldSaveAndReturnResponse() {
        // Arrange
        when(vehiculeRepository.save(any(Vehicule.class))).thenReturn(vehicule);

        // Act
        VehiculeResponse response = vehiculeService.createVehicule(request);

        // Assert
        assertNotNull(response);
        assertEquals(vehiculeId, response.getId());
        assertEquals("BUS-G7-001", response.getImmatriculation());
        assertEquals(Vehicule.StatutVehicule.DISPONIBLE, response.getStatut());
        verify(vehiculeRepository, times(1)).save(any(Vehicule.class));
    }

    @Test
    void getVehiculeById_WhenExists_ShouldReturnResponse() {
        // Arrange
        when(vehiculeRepository.findById(vehiculeId)).thenReturn(Optional.of(vehicule));

        // Act
        VehiculeResponse response = vehiculeService.getVehiculeById(vehiculeId);

        // Assert
        assertNotNull(response);
        assertEquals(vehiculeId, response.getId());
        assertEquals("BUS-G7-001", response.getImmatriculation());
    }

    @Test
    void getVehiculeById_WhenNotExists_ShouldThrowException() {
        // Arrange
        when(vehiculeRepository.findById(vehiculeId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(VehiculeNotFoundException.class, () -> {
            vehiculeService.getVehiculeById(vehiculeId);
        });
    }
}
