package com.sgitu.g4.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class IncidentImpactResponse {

	private Long id;
	private Long missionId;
	private String g9ReferenceIncident;
	private String vehiculeId;
	private String g9Type;
	private String g9Statut;
	private String description;
	private String payloadJson;
	private Instant occurredAt;
	private Instant createdAt;
}
