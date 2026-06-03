package com.sgitu.g4.service;

import com.sgitu.g4.dto.NotificationSendRequest;
import com.sgitu.g4.dto.NotificationSendResponse;
import com.sgitu.g4.integration.G3UserClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

/**
 * Contrat G5 : un {@code POST /api/notifications/send} par utilisateur, chaque payload contient {@code recipient}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class G5RecipientBroadcastService {

	private final G3UserClient g3UserClient;
	private final NotificationDispatchService notificationDispatchService;
	private final SupervisionLogService supervisionLogService;

	/**
	 * Si {@code recipient} est déjà renseigné → envoi unique. Sinon → liste G3 puis un POST par utilisateur.
	 */
	public NotificationSendResponse dispatch(NotificationSendRequest template) {
		if (template.getRecipient() != null
				&& StringUtils.hasText(template.getRecipient().getEmail())
				&& StringUtils.hasText(template.getRecipient().getUserId())) {
			return notificationDispatchService.send(template);
		}
		broadcast(template);
		return NotificationSendResponse.builder()
				.status("ACCEPTED")
				.correlationId(template.effectiveNotificationId())
				.detail("Notification(s) G5 envoyée(s) avec recipient (G3)")
				.build();
	}

	public void broadcast(NotificationSendRequest template) {
		List<G3UserClient.NotificationRecipient> recipients = g3UserClient.listNotificationRecipients();
		if (recipients.isEmpty()) {
			log.error("Aucun destinataire G3 — impossible d'envoyer G5 avec recipient (eventType={})",
					template.getEventType());
			supervisionLogService.add("ERROR", "G5-BROADCAST",
					"Aucun destinataire G3 — POST G5 annulé pour " + template.getEventType());
			return;
		}
		int sent = 0;
		for (G3UserClient.NotificationRecipient recipient : recipients) {
			NotificationSendRequest perUser = copyTemplate(template);
			perUser.setNotificationId(UUID.randomUUID().toString());
			NotificationSendRequest.Recipient r = new NotificationSendRequest.Recipient();
			r.setUserId(recipient.userId());
			r.setEmail(recipient.email());
			perUser.setRecipient(r);
			notificationDispatchService.send(perUser);
			sent++;
		}
		supervisionLogService.add("INFO", "G5-BROADCAST",
				"POST G5 " + template.getEventType() + " × " + sent + " destinataire(s)");
		log.info("Broadcast G5 {} → {} destinataire(s)", template.getEventType(), sent);
	}

	private static NotificationSendRequest copyTemplate(NotificationSendRequest template) {
		NotificationSendRequest copy = new NotificationSendRequest();
		copy.setSourceService(template.getSourceService());
		copy.setEventType(template.getEventType());
		copy.setChannel(template.getChannel());
		if (template.getMetadata() != null) {
			NotificationSendRequest.Metadata metadata = new NotificationSendRequest.Metadata();
			metadata.setLineId(template.getMetadata().getLineId());
			metadata.setReason(template.getMetadata().getReason());
			if (template.getMetadata().getVariables() != null) {
				metadata.setVariables(new java.util.LinkedHashMap<>(template.getMetadata().getVariables()));
			}
			copy.setMetadata(metadata);
		}
		return copy;
	}
}
