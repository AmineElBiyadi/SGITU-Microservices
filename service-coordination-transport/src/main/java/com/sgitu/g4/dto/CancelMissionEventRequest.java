package com.sgitu.g4.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CancelMissionEventRequest {

	@NotNull
	private Long missionId;

	@Size(max = 2000)
	private String motif;
	private boolean notifierG3;
}
