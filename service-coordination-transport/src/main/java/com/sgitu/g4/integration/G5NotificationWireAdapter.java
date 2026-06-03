package com.sgitu.g4.integration;

import com.sgitu.g4.dto.NotificationSendRequest;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adaptateur fil HTTP G10→G5 : JSON métier G4 + champs techniques G5 ({@code priority}, {@code wireSourceService}).
 * {@code recipient} inclus lorsque G4 remplit le champ (broadcast G3→G5).
 */
@Component
public class G5NotificationWireAdapter {

	public Map<String, Object> toWirePayload(NotificationSendRequest contract) {
		Map<String, Object> variables = contract.getMetadata().getVariables() != null
				? new LinkedHashMap<>(contract.getMetadata().getVariables())
				: new LinkedHashMap<>();

		Map<String, Object> metadata = new LinkedHashMap<>();
		metadata.put("lineId", contract.getMetadata().getLineId());
		metadata.put("reason", contract.getMetadata().getReason());
		metadata.put("variables", variables);
		variables.forEach(metadata::putIfAbsent);

		Map<String, Object> wire = new LinkedHashMap<>();
		wire.put("notificationId", contract.effectiveNotificationId());
		wire.put("sourceService", contract.getSourceService());
		wire.put("eventType", contract.getEventType());
		wire.put("channel", contract.getChannel());
		wire.put("priority", "NORMAL");
		if (contract.getRecipient() != null) {
			Map<String, Object> recipient = new LinkedHashMap<>();
			recipient.put("userId", contract.getRecipient().getUserId());
			recipient.put("email", contract.getRecipient().getEmail());
			wire.put("recipient", recipient);
		}
		wire.put("metadata", metadata);
		return wire;
	}
}
