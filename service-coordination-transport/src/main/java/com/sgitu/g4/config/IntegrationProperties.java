package com.sgitu.g4.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "sgitu.integration")
public class IntegrationProperties {

	/** Service utilisateurs (G3). */
	private String g3BaseUrl = "http://localhost:8083";
	private boolean g3ValidationEnabled = true;
	private boolean g3ValidationStrict = false;
	/** Contrat G3 : GET /api/users/drivers/ids (context-path /api sur G3). */
	private String g3DriversIdsPath = "/api/users/drivers/ids";

	/** Contrat G3→G4→G5 : GET /api/users/notification-recipients */
	private boolean g3NotificationRecipientsEnabled = true;
	private boolean g3NotificationRecipientsStrict = false;
	private String g3NotificationRecipientsPath = "/api/users/notification-recipients";
	private int g3NotificationRecipientsPageSize = 100;
	/** Optionnel, ex. ROLE_DISPATCHER,ROLE_SUPERVISOR */
	private String g3NotificationRecipientRoles = "";
	/**
	 * JWT compte {@code ROLE_G4_OPERATOR} pour appels G3 hors requête HTTP (Kafka retard/déviation, etc.).
	 * Obtenir via POST G3 {@code /api/auth/login} ; ne pas committer en clair en prod.
	 */
	private String g3ServiceBearerToken = "";

	/** URL billetterie (G1). */
	private String g1BaseUrl = "http://localhost:8081";
	private String g5BaseUrl = "http://localhost:8085";
	private String g7BaseUrl = "http://localhost:8087";
	/** Flow G7 : vehicle.registered → affectation ligne → EN_SERVICE → mission. */
	private boolean g7FlowEnabled = true;
	private boolean g7FlowStrict = false;
	/** Si référentiel vide, tente GET G7 avant de refuser (mode non strict). */
	private boolean g7SyncOnDemandEnabled = true;
	private String g7VehiculesPath = "/api/suivi-vehicules/vehicules";
	private String g9BaseUrl = "http://localhost:8089";
	private String g10GatewayUrl = "http://localhost:8080";
	private int connectTimeoutMs = 3000;
	private int readTimeoutMs = 5000;
	private String g5NotificationPath = "/api/notifications/send";
	private String g1MissionLifecyclePath = "/api/internal/missions/events";
}
