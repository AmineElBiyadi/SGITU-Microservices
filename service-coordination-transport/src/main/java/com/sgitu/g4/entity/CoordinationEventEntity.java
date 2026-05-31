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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "coordination_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoordinationEventEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private CoordinationEventType type;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	@Builder.Default
	private CoordinationEventStatus status = CoordinationEventStatus.SIGNALE;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "mission_id")
	private Mission mission;

	@Column(length = 64)
	private String vehiculeId;

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
