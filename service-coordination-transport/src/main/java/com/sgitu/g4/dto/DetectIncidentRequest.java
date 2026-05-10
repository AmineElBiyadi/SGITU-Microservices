package com.sgitu.g4.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DetectIncidentRequest {

	@NotBlank
	@Size(max = 128)
	private String incidentReference;
	private Long missionId;

	@NotBlank
	@Size(max = 64)
	private String vehiculeId;

	@NotBlank
	@Size(max = 4000)
	private String resume;
}
