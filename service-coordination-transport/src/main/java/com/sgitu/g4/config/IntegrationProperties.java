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

	private String g3BaseUrl = "http://localhost:8083";
	private String g5BaseUrl = "http://localhost:8085";
	private String g7BaseUrl = "http://localhost:8087";
	private String g9BaseUrl = "http://localhost:8089";
	private String g10GatewayUrl = "http://localhost:8080";
	private int connectTimeoutMs = 3000;
	private int readTimeoutMs = 5000;
	private String g5NotificationPath = "/api/notifications/send";
	private String g3MissionLifecyclePath = "/api/internal/missions/events";
}
