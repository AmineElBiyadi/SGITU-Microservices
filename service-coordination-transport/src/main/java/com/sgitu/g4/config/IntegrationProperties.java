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

	/** URL billetterie (G1). */
	private String g1BaseUrl = "http://localhost:8081";
	private String g5BaseUrl = "http://localhost:8085";
	private String g7BaseUrl = "http://localhost:8087";
	private String g9BaseUrl = "http://localhost:8089";
	private String g10GatewayUrl = "http://localhost:8080";
	private int connectTimeoutMs = 3000;
	private int readTimeoutMs = 5000;
	private String g5NotificationPath = "/api/notifications/send";
	private String g1MissionLifecyclePath = "/api/internal/missions/events";
}
