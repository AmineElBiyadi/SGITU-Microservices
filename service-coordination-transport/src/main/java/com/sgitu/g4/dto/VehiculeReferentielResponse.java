package com.sgitu.g4.dto;

import com.sgitu.g4.entity.StatutVehiculeG7;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehiculeReferentielResponse {

	private String vehiculeId;
	private String immatriculation;
	private String typeVehicule;
	private StatutVehiculeG7 statutG7;
	private boolean disponiblePourAffectation;
	private Long ligneAffecteeId;
	private Instant registeredAt;
	private Instant updatedAt;
}
