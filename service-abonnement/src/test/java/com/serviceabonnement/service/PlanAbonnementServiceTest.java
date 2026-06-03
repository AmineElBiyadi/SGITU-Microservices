package com.serviceabonnement.service;

import com.serviceabonnement.entity.PlanAbonnement;
import com.serviceabonnement.repository.PlanAbonnementRepository;
import com.serviceabonnement.exception.PlanNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanAbonnementServiceTest {

    @Mock
    private PlanAbonnementRepository planRepository;

    @InjectMocks
    private PlanAbonnementService planService;

    private PlanAbonnement samplePlan;

    @BeforeEach
    void setUp() {
        samplePlan = new PlanAbonnement();
        samplePlan.setIdPlan(1L);
        samplePlan.setNomPlan("Forfait Étudiant");
        samplePlan.setPrix(150.0);
    }

    @Test
    void testGetPlanById_Success() {
        // Arrange : On configure le comportement de notre Mock (faux repository)
        when(planRepository.findById(1L)).thenReturn(Optional.of(samplePlan));

        // Act : On exécute la méthode du vrai service
        PlanAbonnement result = planService.getPlanById(1L);

        // Assert : On vérifie les résultats (Assertions)
        assertNotNull(result);
        assertEquals("Forfait Étudiant", result.getNomPlan());
        assertEquals(150.0, result.getPrix());
        
        // On vérifie que la méthode findById a bien été appelée 1 seule fois
        verify(planRepository, times(1)).findById(1L);
    }

    @Test
    void testGetPlanById_NotFound() {
        // Arrange : Le mock ne trouve rien
        when(planRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert : On vérifie que l'exception PlanNotFoundException est bien déclenchée
        assertThrows(PlanNotFoundException.class, () -> planService.getPlanById(99L));
        verify(planRepository, times(1)).findById(99L);
    }

    @Test
    void testGetAllPlans() {
        // Arrange
        when(planRepository.findAll()).thenReturn(Arrays.asList(samplePlan, new PlanAbonnement()));

        // Act
        List<PlanAbonnement> result = planService.getAllPlans();

        // Assert
        assertEquals(2, result.size());
        verify(planRepository, times(1)).findAll();
    }

    @Test
    void testCreatePlan() {
        // Arrange
        when(planRepository.save(any(PlanAbonnement.class))).thenReturn(samplePlan);

        // Act
        PlanAbonnement result = planService.createPlan(samplePlan);

        // Assert
        assertNotNull(result);
        assertEquals("Forfait Étudiant", result.getNomPlan());
        verify(planRepository, times(1)).save(samplePlan);
    }

    @Test
    void testUpdatePlan() {
        // Arrange
        PlanAbonnement newDetails = new PlanAbonnement();
        newDetails.setNomPlan("Forfait Premium");
        newDetails.setPrix(300.0);

        // Quand on cherche l'ID 1, on trouve notre samplePlan
        when(planRepository.findById(1L)).thenReturn(Optional.of(samplePlan));
        // Quand on sauvegarde, on retourne le plan (qui aura été modifié par le service)
        when(planRepository.save(any(PlanAbonnement.class))).thenReturn(samplePlan);

        // Act
        PlanAbonnement result = planService.updatePlan(1L, newDetails);

        // Assert
        assertEquals("Forfait Premium", result.getNomPlan());
        assertEquals(300.0, result.getPrix());
        verify(planRepository, times(1)).findById(1L);
        verify(planRepository, times(1)).save(samplePlan);
    }

    @Test
    void testDeletePlan() {
        // Act
        planService.deletePlan(1L);

        // Assert
        // On vérifie que la méthode deleteById a bien été appelée 1 fois avec l'ID 1
        verify(planRepository, times(1)).deleteById(1L);
    }
}
