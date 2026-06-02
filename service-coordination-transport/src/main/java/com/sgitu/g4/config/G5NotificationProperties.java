package com.sgitu.g4.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Après événement coordination / incident G9 confirmé : appel du contrat REST
 * {@code POST /api/notifications/send} (via {@link com.sgitu.g4.service.NotificationDispatchService}).
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "sgitu.g5-notification")
public class G5NotificationProperties {

	/** Si true, G4 déclenche le POST contrat G5 (un envoi par utilisateur, recipient via G3). */
	private boolean postOnEventEnabled = true;

	private String sourceService = "COORDINATION";

	private String channel = "EMAIL";
}
