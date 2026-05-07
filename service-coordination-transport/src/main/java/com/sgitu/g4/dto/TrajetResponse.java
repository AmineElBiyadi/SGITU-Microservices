package com.sgitu.g4.dto;

import com.sgitu.g4.entity.DirectionTrajet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrajetResponse {

	private Long id;
	private Long ligneId;
	private String code;
	private String nom;
	private DirectionTrajet sens;
	private boolean actif;
	private List<ArretResponse> arrets;
	private Instant createdAt;
	private Instant updatedAt;
}
