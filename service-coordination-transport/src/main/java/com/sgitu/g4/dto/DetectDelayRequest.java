package com.sgitu.g4.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class DetectDelayRequest {

	@NotNull
	private Long missionId;

	@NotNull
	@Positive
	private Integer retardMinutes;
	private String cause;
}
