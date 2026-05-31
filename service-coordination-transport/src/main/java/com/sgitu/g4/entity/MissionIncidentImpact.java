package com.sgitu.g4.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Lien G4 entre une {@link Mission} et un incident métier géré par G9 ({@code g9ReferenceIncident}).
 * Ne duplique pas l'entité Incident du microservice G9.
 */
@Entity
@Table(name = "mission_incident_impacts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MissionIncidentImpact {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "mission_id")
	private Mission mission;

	@Column(name = "g9_reference_incident", nullable = false, length = 128)
	private String g9ReferenceIncident;

	@Column(length = 64)
	private String vehiculeId;

	@Column(name = "g9_type", length = 64)
	private String g9Type;

	@Column(name = "g9_statut", length = 64)
	private String g9Statut;

	@Column(length = 4000)
	private String description;

	@Column(columnDefinition = "TEXT")
	private String payloadJson;

	@Column(nullable = false)
	private Instant occurredAt;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@PrePersist
	void prePersist() {
		Instant now = Instant.now();
		if (occurredAt == null) {
			occurredAt = now;
		}
		createdAt = now;
	}
}
