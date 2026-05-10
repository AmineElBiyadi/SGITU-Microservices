package com.sgitu.g4.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgitu.g4.dto.LigneRequest;
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
class LigneApiIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void health_estPublic() throws Exception {
		mockMvc.perform(get("/api/g4/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").exists());
	}

	@Test
	void lignes_requiertAuthentification() throws Exception {
		mockMvc.perform(get("/api/g4/lignes"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "gestionnaire.reseau", roles = { "G4_OPERATOR" })
	void creationLigne_nominal() throws Exception {
		LigneRequest req = new LigneRequest();
		req.setCode("INT-L1");
		req.setNom("Ligne test");
		req.setDescription("desc");
		req.setActive(true);

		mockMvc.perform(post("/api/g4/lignes")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(req)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.code").value("INT-L1"));

		mockMvc.perform(get("/api/g4/lignes"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].code").value("INT-L1"));
	}
}
