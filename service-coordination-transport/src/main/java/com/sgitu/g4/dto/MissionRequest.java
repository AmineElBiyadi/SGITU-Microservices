package com.sgitu.g4.dto;

import com.sgitu.g4.entity.StatutMission;
import io.swagger.v3.oas.annotations.media.Schema;
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
	@Schema(
			description = "Identifiant conducteur (driver id) fourni par le service Gestion des utilisateurs (G3). "
					+ "Même valeur qu'un élément renvoyé par GET /api/users/drivers/ids côté G3. "
					+ "Optionnel ; G4 ne vérifie pas l'existence chez G3 (référence opaque, max 64 caractères).")
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
