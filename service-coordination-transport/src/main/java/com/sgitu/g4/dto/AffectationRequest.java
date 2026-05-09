package com.sgitu.g4.dto;

import com.sgitu.g4.entity.StatutAffectation;
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
