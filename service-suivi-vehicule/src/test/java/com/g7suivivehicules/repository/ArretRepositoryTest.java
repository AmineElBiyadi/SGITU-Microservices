package com.g7suivivehicules.repository;

import com.g7suivivehicules.entity.Arret;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
class ArretRepositoryTest {

    @Autowired
    private ArretRepository arretRepository;

    @Test
    void findArretsDansRayon_ShouldReturnOnlyArretsWithinRadius() {
        // Arrange
        UUID referenceId = UUID.randomUUID();
        UUID vehiculeId = UUID.randomUUID();

        // 1. Arrêt proche (environ 10 mètres d'écart)
        Arret proche = Arret.builder()
                .arretId(referenceId)
                .vehiculeId(vehiculeId)
                .timestamp(LocalDateTime.now())
                .present(true)
                .latitude(48.8566)
                .longitude(2.3522)
                .nom("Arrêt Proche")
                .build();

        // 2. Arrêt éloigné (environ 2 kilomètres d'écart)
        Arret eloigne = Arret.builder()
                .arretId(referenceId)
                .vehiculeId(vehiculeId)
                .timestamp(LocalDateTime.now())
                .present(true)
                .latitude(48.8738)
                .longitude(2.3298)
                .nom("Arrêt Éloigné")
                .build();

        arretRepository.save(proche);
        arretRepository.save(eloigne);

        // Act
        // Recherche dans un rayon de 100 mètres autour de (48.8566, 2.3522)
        List<Arret> result = arretRepository.findArretsDansRayon(48.8566, 2.3522, 100.0);

        // Assert
        assertEquals(1, result.size());
        assertEquals("Arrêt Proche", result.get(0).getNom());
    }

    @Test
    void findArretsDansRayon_WhenNoArretInRadius_ShouldReturnEmptyList() {
        // Act
        List<Arret> result = arretRepository.findArretsDansRayon(48.8566, 2.3522, 100.0);

        // Assert
        assertTrue(result.isEmpty());
    }
}
