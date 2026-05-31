package com.sgitu.g4.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "sgitu.kafka")
public class KafkaAppProperties {

	private boolean enabled = false;
	private String topicCoordination = "sgitu.g4.coordination.events";
	private String topicIncidentInbound = "incident.transport.topic";
	private String g9ConsumerGroupId = "g4-coordination-g9";
	private String topicMissionLifecycle = "missions-lifecycle";
	private String g7PositionsTopic = "vehicule-positions";
	private String g7ConsumerGroupId = "g4-coordination-g7";
}
