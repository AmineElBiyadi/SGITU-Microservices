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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
	name = "trajet_arret",
	uniqueConstraints = @UniqueConstraint(name = "uk_trajet_sequence", columnNames = { "trajet_id", "sequence_order" })
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrajetArret {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "trajet_id")
	private Trajet trajet;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "arret_id")
	private Arret arret;

	@Column(name = "sequence_order", nullable = false)
	private Integer sequenceOrder;

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
