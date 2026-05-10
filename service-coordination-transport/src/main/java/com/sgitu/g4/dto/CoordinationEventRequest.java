package com.sgitu.g4.dto;

import com.sgitu.g4.entity.CoordinationEventStatus;
import com.sgitu.g4.entity.CoordinationEventType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;

@Data
public class CoordinationEventRequest {

	@NotNull
	private CoordinationEventType type;
	private CoordinationEventStatus status;
	private Long missionId;

	@Size(max = 64)
	private String vehiculeId;

	@Size(max = 4000)
	private String description;
	private String payloadJson;
	private Instant occurredAt;
}
