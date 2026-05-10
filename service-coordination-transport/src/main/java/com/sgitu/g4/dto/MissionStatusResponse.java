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
public class MissionStatusResponse {

	private Long missionId;
	private StatutMission statut;
	private Instant lastUpdate;
	private String vehiculeId;
	private Long ligneId;
}
