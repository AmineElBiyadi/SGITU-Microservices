package com.sgitu.g4.dto;

import com.sgitu.g4.entity.StatutMission;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MissionResponse {

	private Long id;
	private String vehiculeId;
	private String chauffeurId;
	private Long ligneId;
	private Long trajetId;
	private Long affectationId;
	private StatutMission statut;
	private Instant plannedStart;
	private Instant actualStart;
	private Instant endedAt;
	private String referenceG3;
	private String notes;
	private Instant createdAt;
	private Instant updatedAt;
}
