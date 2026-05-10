package com.sgitu.g4.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "affectations_vehicule_ligne")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AffectationVehiculeLigne {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 64)
	private String vehiculeId;

	/** Identifiant conducteur fourni par G3 (gestion des utilisateurs / driver id) ; G4 ne stocke que cette référence opaque. */
	@Column(length = 64)
	private String chauffeurId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ligne_id")
	private Ligne ligne;

	@Column(nullable = false)
	private Instant dateDebut;

	private Instant dateFin;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private StatutAffectation statut;

	@Column(length = 1000)
	private String commentaire;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	private Instant updatedAt;

	@PrePersist
	void prePersist() {
		Instant now = Instant.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = Instant.now();
	}
}
