package com.sgitu.g4.mapper;

import com.sgitu.g4.dto.AffectationResponse;
import com.sgitu.g4.dto.ArretResponse;
import com.sgitu.g4.dto.CoordinationEventResponse;
import com.sgitu.g4.dto.HoraireResponse;
import com.sgitu.g4.dto.LigneResponse;
import com.sgitu.g4.dto.MissionResponse;
import com.sgitu.g4.dto.MissionStatusResponse;
import com.sgitu.g4.dto.TrajetResponse;
import com.sgitu.g4.entity.AffectationVehiculeLigne;
import com.sgitu.g4.entity.Arret;
import com.sgitu.g4.entity.CoordinationEventEntity;
import com.sgitu.g4.entity.Horaire;
import com.sgitu.g4.entity.Ligne;
import com.sgitu.g4.entity.Mission;
import com.sgitu.g4.entity.Trajet;
import com.sgitu.g4.entity.TrajetArret;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class EntityMapper {

	private EntityMapper() {
	}

	public static LigneResponse toDto(Ligne e) {
		if (e == null) {
			return null;
		}
		return LigneResponse.builder()
				.id(e.getId())
				.code(e.getCode())
				.nom(e.getNom())
				.description(e.getDescription())
				.active(e.isActive())
				.createdAt(e.getCreatedAt())
				.updatedAt(e.getUpdatedAt())
				.build();
	}

	public static ArretResponse toDto(Arret e) {
		if (e == null) {
			return null;
		}
		return ArretResponse.builder()
				.id(e.getId())
				.code(e.getCode())
				.nom(e.getNom())
				.latitude(e.getLatitude())
				.longitude(e.getLongitude())
				.ligneId(e.getLigne() != null ? e.getLigne().getId() : null)
				.createdAt(e.getCreatedAt())
				.updatedAt(e.getUpdatedAt())
				.build();
	}

	public static List<ArretResponse> arretsSortedFromTrajet(Trajet t) {
		if (t == null || t.getArretsSequence() == null) {
			return List.of();
		}
		return t.getArretsSequence().stream()
				.sorted(Comparator.comparing(TrajetArret::getSequenceOrder))
				.map(TrajetArret::getArret)
				.map(EntityMapper::toDto)
				.collect(Collectors.toList());
	}

	public static TrajetResponse toDto(Trajet e) {
		if (e == null) {
			return null;
		}
		return TrajetResponse.builder()
				.id(e.getId())
				.ligneId(e.getLigne() != null ? e.getLigne().getId() : null)
				.code(e.getCode())
				.nom(e.getNom())
				.sens(e.getSens())
				.actif(e.isActif())
				.arrets(arretsSortedFromTrajet(e))
				.createdAt(e.getCreatedAt())
				.updatedAt(e.getUpdatedAt())
				.build();
	}

	public static HoraireResponse toDto(Horaire e) {
		if (e == null) {
			return null;
		}
		return HoraireResponse.builder()
				.id(e.getId())
				.trajetId(e.getTrajet() != null ? e.getTrajet().getId() : null)
				.arretId(e.getArret() != null ? e.getArret().getId() : null)
				.heurePassage(e.getHeurePassage())
				.jourSemaine(e.getJourSemaine())
				.validFrom(e.getValidFrom())
				.validTo(e.getValidTo())
				.libelle(e.getLibelle())
				.createdAt(e.getCreatedAt())
				.updatedAt(e.getUpdatedAt())
				.build();
	}

	public static AffectationResponse toDto(AffectationVehiculeLigne e) {
		if (e == null) {
			return null;
		}
		return AffectationResponse.builder()
				.id(e.getId())
				.vehiculeId(e.getVehiculeId())
				.chauffeurId(e.getChauffeurId())
				.ligneId(e.getLigne() != null ? e.getLigne().getId() : null)
				.dateDebut(e.getDateDebut())
				.dateFin(e.getDateFin())
				.statut(e.getStatut())
				.commentaire(e.getCommentaire())
				.createdAt(e.getCreatedAt())
				.updatedAt(e.getUpdatedAt())
				.build();
	}

	public static MissionResponse toDto(Mission e) {
		if (e == null) {
			return null;
		}
		return MissionResponse.builder()
				.id(e.getId())
				.vehiculeId(e.getVehiculeId())
				.chauffeurId(e.getChauffeurId())
				.ligneId(e.getLigne() != null ? e.getLigne().getId() : null)
				.trajetId(e.getTrajet() != null ? e.getTrajet().getId() : null)
				.affectationId(e.getAffectation() != null ? e.getAffectation().getId() : null)
				.statut(e.getStatut())
				.plannedStart(e.getPlannedStart())
				.actualStart(e.getActualStart())
				.endedAt(e.getEndedAt())
				.notes(e.getNotes())
				.createdAt(e.getCreatedAt())
				.updatedAt(e.getUpdatedAt())
				.build();
	}

	public static MissionStatusResponse toStatus(Mission e) {
		if (e == null) {
			return null;
		}
		return MissionStatusResponse.builder()
				.missionId(e.getId())
				.statut(e.getStatut())
				.lastUpdate(e.getUpdatedAt())
				.vehiculeId(e.getVehiculeId())
				.ligneId(e.getLigne() != null ? e.getLigne().getId() : null)
				.build();
	}

	public static CoordinationEventResponse toDto(CoordinationEventEntity e) {
		if (e == null) {
			return null;
		}
		return CoordinationEventResponse.builder()
				.id(e.getId())
				.type(e.getType())
				.status(e.getStatus())
				.missionId(e.getMission() != null ? e.getMission().getId() : null)
				.vehiculeId(e.getVehiculeId())
				.description(e.getDescription())
				.payloadJson(e.getPayloadJson())
				.occurredAt(e.getOccurredAt())
				.createdAt(e.getCreatedAt())
				.build();
	}
}
