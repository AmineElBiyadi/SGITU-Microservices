package com.sgitu.g4.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class G3MissionLifecycleMessage {

	private String notificationId;
	private String eventType;
	private Metadata metadata;

	@Data
	@Builder
	public static class Metadata {
		private String reason;
		private MissionDetails missionDetails;
		private Map<String, Object> variables;
	}

	@Data
	@Builder
	public static class MissionDetails {
		private String missionId;
		private String status;
		private Map<String, String> horaire;
		private Map<String, String> trajet;
	}
}
