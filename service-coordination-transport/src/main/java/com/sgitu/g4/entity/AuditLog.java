package com.sgitu.g4.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Instant timestamp;

	@Column(nullable = false, length = 16)
	private String level;

	@Column(nullable = false, length = 64)
	private String source;

	@Column(nullable = false, length = 4000)
	private String message;

	@PrePersist
	void prePersist() {
		if (timestamp == null) {
			timestamp = Instant.now();
		}
	}
}
