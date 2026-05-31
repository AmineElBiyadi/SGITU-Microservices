package com.g7suivivehicules.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.g7suivivehicules.dto.VehiculeRequest;
import com.g7suivivehicules.dto.VehiculeResponse;
import com.g7suivivehicules.entity.Vehicule;
import com.g7suivivehicules.security.HeaderAuthFilter;
import com.g7suivivehicules.security.SecurityConfig;
import com.g7suivivehicules.service.SnapshotService;
import com.g7suivivehicules.service.VehiculeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VehiculeController.class)
@Import({SecurityConfig.class, HeaderAuthFilter.class})
@ActiveProfiles("test")
class VehiculeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VehiculeService vehiculeService;

    @MockBean
    private SnapshotService snapshotService;

    @Test
    void ajouterVehicule_WithAdminRole_ShouldReturnCreated() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();
        VehiculeRequest request = VehiculeRequest.builder()
                .immatriculation("BUS-G7-001")
                .type(Vehicule.TypeVehicule.BUS)
                .ligne("G4")
                .conducteurId(UUID.randomUUID())
                .build();

        VehiculeResponse response = VehiculeResponse.builder()
                .id(id)
                .immatriculation("BUS-G7-001")
                .type(Vehicule.TypeVehicule.BUS)
                .ligne("G4")
                .statut(Vehicule.StatutVehicule.DISPONIBLE)
                .conducteurId(request.getConducteurId())
                .build();

        when(vehiculeService.createVehicule(any(VehiculeRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/suivi-vehicules/vehicules")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-Roles", "ROLE_ADMIN_G7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.immatriculation").value("BUS-G7-001"));
    }

    @Test
    void ajouterVehicule_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        // Arrange
        VehiculeRequest invalidRequest = VehiculeRequest.builder()
                .immatriculation("") // Invalide : obligatoire vide
                .type(null) // Invalide : obligatoire null
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/suivi-vehicules/vehicules")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-Roles", "ROLE_ADMIN_G7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ajouterVehicule_WithoutAdminRole_ShouldReturnForbidden() throws Exception {
        // Arrange
        VehiculeRequest request = VehiculeRequest.builder()
                .immatriculation("BUS-G7-001")
                .type(Vehicule.TypeVehicule.BUS)
                .ligne("G4")
                .build();

        // Act & Assert (Driver cannot create vehicles, only Admin G7 can)
        mockMvc.perform(post("/api/suivi-vehicules/vehicules")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-Roles", "ROLE_DRIVER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
