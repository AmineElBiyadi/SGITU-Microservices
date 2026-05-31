package com.sgitu.g4.entity;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trajets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trajet {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ligne_id")
	private Ligne ligne;

	@Column(nullable = false, length = 64)
	private String code;

	@Column(nullable = false, length = 200)
	private String nom;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private DirectionTrajet sens;

	@Column(nullable = false)
	@Builder.Default
	private boolean actif = true;

	@OneToMany(mappedBy = "trajet", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("sequenceOrder ASC")
	@Builder.Default
	private List<TrajetArret> arretsSequence = new ArrayList<>();

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
