package com.sgitu.g4.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DetectBreakdownRequest {

	@NotBlank
	@Size(max = 64)
	private String vehiculeId;
	private Long missionId;

	@NotBlank
	@Size(max = 2000)
	private String symptomes;
}
