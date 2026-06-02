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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MissionVehicleConflictIntegrationTest {

	private static final String VEHICULE_UUID = "00000000-0000-4000-8000-000000000001";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void logs_estPublic() throws Exception {
		mockMvc.perform(get("/api/g4/logs"))
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(username = "admin.technique", roles = { "G4_OPERATOR", "DISPATCHER" })
	void deuxMissionsEnCoursMemeVehicule_retourne409() throws Exception {
		LigneRequest ligne = new LigneRequest();
		ligne.setCode("MIS-L1");
		ligne.setNom("Ligne mission test");
		ligne.setActive(true);
		String ligneBody = mockMvc.perform(post("/api/g4/lignes")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(ligne)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		Long ligneId = objectMapper.readTree(ligneBody).get("id").asLong();

		MissionRequest first = missionRequest(ligneId, VEHICULE_UUID, StatutMission.EN_COURS);
		mockMvc.perform(post("/api/g4/missions")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(first)))
				.andExpect(status().isCreated());

		MissionRequest second = missionRequest(ligneId, VEHICULE_UUID, StatutMission.EN_COURS);
		mockMvc.perform(post("/api/g4/missions")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(second)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("CONFLICT"));
	}

	private static MissionRequest missionRequest(Long ligneId, String vehiculeId, StatutMission statut) {
		MissionRequest req = new MissionRequest();
		req.setVehiculeId(vehiculeId);
		req.setLigneId(ligneId);
		req.setStatut(statut);
		return req;
	}
}
