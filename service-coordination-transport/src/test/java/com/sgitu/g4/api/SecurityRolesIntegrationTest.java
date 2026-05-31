package com.sgitu.g4.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgitu.g4.dto.LigneRequest;
import com.sgitu.g4.dto.MissionRequest;
import com.sgitu.g4.entity.StatutMission;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityRolesIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void logs_sontPublics() throws Exception {
		mockMvc.perform(get("/api/g4/logs")).andExpect(status().isOk());
	}

	@Test
	@WithMockUser(username = "gestionnaire.flotte", roles = { "DISPATCHER" })
	void dispatcher_nePeutPasCreerLigne() throws Exception {
		LigneRequest req = new LigneRequest();
		req.setCode("SEC-L1");
		req.setNom("Interdit");
		req.setActive(true);
		mockMvc.perform(post("/api/g4/lignes")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(req)))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "gestionnaire.reseau", roles = { "G4_OPERATOR" })
	void operator_nePeutPasCreerMission() throws Exception {
		MissionRequest req = new MissionRequest();
		req.setVehiculeId("VH-SEC");
		req.setLigneId(1L);
		req.setStatut(StatutMission.PLANIFIEE);
		mockMvc.perform(post("/api/g4/missions")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(req)))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "gestionnaire.flotte", roles = { "DISPATCHER" })
	void dispatcher_peutLireMissions() throws Exception {
		mockMvc.perform(get("/api/g4/missions")).andExpect(status().isOk());
	}
}
