package com.sgitu.g4.dto;

import com.sgitu.g4.entity.StatutMission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;

@Data
public class MissionRequest {

	@NotBlank
	@Size(max = 64)
	private String vehiculeId;

	@Size(max = 64)
	private String chauffeurId;

	@NotNull
	private Long ligneId;
	private Long trajetId;
	private Long affectationId;

	@NotNull
	private StatutMission statut;

	private Instant plannedStart;
	private Instant actualStart;
	private Instant endedAt;

	@Size(max = 128)
	private String referenceG3;

	@Size(max = 2000)
	private String notes;
}
