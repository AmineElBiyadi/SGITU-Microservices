package com.sgitu.g4.dto;

import com.sgitu.g4.entity.CoordinationEventStatus;
import com.sgitu.g4.entity.CoordinationEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoordinationEventResponse {

	private Long id;
	private CoordinationEventType type;
	private CoordinationEventStatus status;
	private Long missionId;
	private String vehiculeId;
	private String description;
	private String payloadJson;
	private Instant occurredAt;
	private Instant createdAt;
}
