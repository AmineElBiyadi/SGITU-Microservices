package com.sgitu.g4.dto;

import com.sgitu.g4.entity.StatutAffectation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AffectationResponse {

	private Long id;
	private String vehiculeId;
	private Long ligneId;
	private Instant dateDebut;
	private Instant dateFin;
	private StatutAffectation statut;
	private String commentaire;
	private Instant createdAt;
	private Instant updatedAt;
}
