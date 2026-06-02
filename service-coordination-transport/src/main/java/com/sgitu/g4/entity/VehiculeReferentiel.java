package com.sgitu.g4.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Référentiel local des véhicules connus via G7 (Kafka {@code vehicle.registered} ou sync REST).
 * G4 ne duplique pas la flotte complète : seulement les métadonnées nécessaires à l'affectation et aux missions.
 */
@Entity
@Table(name = "vehicules_referentiel")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehiculeReferentiel {

	@Id
	@Column(name = "vehicule_id", length = 64)
	private String vehiculeId;

	@Column(length = 32)
	private String immatriculation;

	@Column(name = "type_vehicule", length = 32)
	private String typeVehicule;

	@Enumerated(EnumType.STRING)
	@Column(name = "statut_g7", nullable = false, length = 32)
	private StatutVehiculeG7 statutG7;

	@Column(name = "disponible_pour_affectation", nullable = false)
	private boolean disponiblePourAffectation;

	@Column(name = "ligne_affectee_id")
	private Long ligneAffecteeId;

	@Column(nullable = false, updatable = false)
	private Instant registeredAt;

	private Instant updatedAt;

	@PrePersist
	void prePersist() {
		Instant now = Instant.now();
		registeredAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = Instant.now();
	}
}
