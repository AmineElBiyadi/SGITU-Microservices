package com.sgitu.g4.notification;

import com.sgitu.g4.dto.NotificationSendRequest;
import com.sgitu.g4.integration.G5NotificationWireAdapter;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class G5NotificationWireAdapterTest {

	private final G5NotificationWireAdapter adapter = new G5NotificationWireAdapter();

	@Test
	void buildsPayloadWithRecipientWhenPresent() {
		NotificationSendRequest contract = new NotificationSendRequest();
		contract.setNotificationId("11111111-1111-4111-8111-111111111111");
		contract.setSourceService("COORDINATION");
		contract.setEventType("DELAY_ALERT");
		contract.setChannel("EMAIL");

		NotificationSendRequest.Metadata metadata = new NotificationSendRequest.Metadata();
		metadata.setLineId("L12");
		metadata.setReason("RETARD_SIGNIFICATIF");
		metadata.setVariables(Map.of("vehiculeId", "v-1", "valeur", "12", "arret", "Gare Sud"));
		contract.setMetadata(metadata);
		NotificationSendRequest.Recipient recipient = new NotificationSendRequest.Recipient();
		recipient.setUserId("42");
		recipient.setEmail("dispatcher@campus.fr");
		contract.setRecipient(recipient);

		@SuppressWarnings("unchecked")
		Map<String, Object> wire = adapter.toWirePayload(contract);

		assertEquals("COORDINATION", wire.get("sourceService"));
		assertEquals("DELAY_ALERT", wire.get("eventType"));
		@SuppressWarnings("unchecked")
		Map<String, Object> wireRecipient = (Map<String, Object>) wire.get("recipient");
		assertEquals("42", wireRecipient.get("userId"));
		assertEquals("dispatcher@campus.fr", wireRecipient.get("email"));
		@SuppressWarnings("unchecked")
		Map<String, Object> wireMeta = (Map<String, Object>) wire.get("metadata");
		assertEquals("RETARD_SIGNIFICATIF", wireMeta.get("reason"));
		assertTrue(wireMeta.containsKey("variables"));
	}
}
