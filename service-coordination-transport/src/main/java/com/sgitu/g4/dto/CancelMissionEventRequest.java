package com.sgitu.g4.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CancelMissionEventRequest {

	@NotNull
	private Long missionId;

	@Size(max = 2000)
	private String motif;
	@JsonAlias("notifierG3")
	private boolean notifierG1;
}
