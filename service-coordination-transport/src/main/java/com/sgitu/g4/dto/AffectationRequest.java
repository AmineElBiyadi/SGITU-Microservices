package com.sgitu.g4.dto;

import com.sgitu.g4.entity.StatutAffectation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;

@Data
public class AffectationRequest {

	@NotBlank
	@Size(max = 64)
	private String vehiculeId;

	@Size(max = 64)
	@Schema(
			description = "Identifiant conducteur (driver id) fourni par G3 (GET /api/users/drivers/ids). "
					+ "Optionnel ; G4 ne vérifie pas l'existence chez G3 (référence opaque, max 64 caractères).")
	private String chauffeurId;

	@NotNull
	private Long ligneId;

	@NotNull
	private Instant dateDebut;
	private Instant dateFin;

	@NotNull
	private StatutAffectation statut;

	@Size(max = 1000)
	private String commentaire;
}
