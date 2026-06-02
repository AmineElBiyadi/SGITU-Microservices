package com.sgitu.g4.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgitu.g4.dto.G3NotificationRecipientsPage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class G3NotificationRecipientsParsingTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void parsesPaginatedRecipientsPage() throws Exception {
		String json = """
				{
				  "items": [
				    { "userId": 1, "email": "a@univ.fr" },
				    { "userId": 2, "email": "b@univ.fr" }
				  ],
				  "page": 0,
				  "size": 100,
				  "total": 2
				}
				""";
		G3NotificationRecipientsPage page = objectMapper.readValue(json, G3NotificationRecipientsPage.class);
		assertEquals(2, page.getItems().size());
		assertEquals(1L, page.getItems().get(0).getUserId());
		assertEquals("a@univ.fr", page.getItems().get(0).getEmail());
		assertEquals(2, page.getTotal());
	}
}
