package com.sgitu.g4.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GatewayHeadersSecurityIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void gatewayHeaders_autorisentLectureMissions() throws Exception {
		mockMvc.perform(get("/api/g4/missions")
						.header("X-User-Id", "42")
						.header("X-User-Email", "dispatcher@univ.fr")
						.header("X-Roles", "DISPATCHER"))
				.andExpect(status().isOk());
	}
}
