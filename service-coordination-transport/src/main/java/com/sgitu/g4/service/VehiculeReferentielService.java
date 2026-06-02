package com.sgitu.g4.service;

import com.sgitu.g4.config.IntegrationProperties;
import com.sgitu.g4.dto.G7VehicleRegisteredMessage;
import com.sgitu.g4.dto.VehiculeReferentielResponse;
import com.sgitu.g4.entity.StatutAffectation;
import com.sgitu.g4.entity.StatutVehiculeG7;
import com.sgitu.g4.entity.VehiculeReferentiel;
import com.sgitu.g4.exception.BadRequestException;
import com.sgitu.g4.integration.G7VehicleClient;
import com.sgitu.g4.mapper.EntityMapper;
import com.sgitu.g4.entity.StatutMission;
import com.sgitu.g4.repository.AffectationRepository;
import com.sgitu.g4.repository.MissionRepository;
import com.sgitu.g4.repository.VehiculeReferentielRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehiculeReferentielService {

	private final VehiculeReferentielRepository vehiculeReferentielRepository;
	private final AffectationRepository affectationRepository;
	private final MissionRepository missionRepository;
	private final G7VehicleClient g7VehicleClient;
	private final IntegrationProperties integrationProperties;
	private final SupervisionLogService supervisionLogService;

	@Transactional
	public VehiculeReferentielResponse registerFromKafka(G7VehicleRegisteredMessage message) {
		String vehiculeId = normalizeVehiculeId(message.getVehiculeId());
		StatutVehiculeG7 statut = parseStatutG7(message.getStatut(), StatutVehiculeG7.DISPONIBLE);
		VehiculeReferentiel entity = vehiculeReferentielRepository.findById(vehiculeId)
				.orElse(VehiculeReferentiel.builder().vehiculeId(vehiculeId).build());
		entity.setImmatriculation(trimToNull(message.getImmatriculation()));
		entity.setTypeVehicule(trimToNull(message.getType()));
		entity.setStatutG7(statut);
		entity.setDisponiblePourAffectation(statut == StatutVehiculeG7.DISPONIBLE);
		if (statut == StatutVehiculeG7.DISPONIBLE) {
			entity.setLigneAffecteeId(null);
		}
		VehiculeReferentiel saved = vehiculeReferentielRepository.save(entity);
		supervisionLogService.add("INFO", "KAFKA-G7-REGISTER",
				"Véhicule enregistré id=" + vehiculeId + " statut=" + statut);
		return EntityMapper.toDto(saved);
	}

	@Transactional
	public VehiculeReferentielResponse syncFromG7(String vehiculeIdRaw) {
		String vehiculeId = normalizeVehiculeId(vehiculeIdRaw);
		Map<String, Object> remote = g7VehicleClient.fetchVehiculeOrThrow(vehiculeId);
		StatutVehiculeG7 statut = parseStatutG7(String.valueOf(remote.get("statut")), StatutVehiculeG7.INCONNU);
		VehiculeReferentiel entity = vehiculeReferentielRepository.findById(vehiculeId)
				.orElse(VehiculeReferentiel.builder().vehiculeId(vehiculeId).build());
		entity.setImmatriculation(stringOrNull(remote.get("immatriculation")));
		entity.setTypeVehicule(stringOrNull(remote.get("type")));
		entity.setStatutG7(statut);
		entity.setDisponiblePourAffectation(statut == StatutVehiculeG7.DISPONIBLE);
		VehiculeReferentiel saved = vehiculeReferentielRepository.save(entity);
		supervisionLogService.add("INFO", "G7-SYNC", "Référentiel synchronisé id=" + vehiculeId);
		return EntityMapper.toDto(saved);
	}

	@Transactional(readOnly = true)
	public List<VehiculeReferentielResponse> listDisponiblesPourAffectation() {
		return vehiculeReferentielRepository.findByDisponiblePourAffectationTrueOrderByRegisteredAtDesc().stream()
				.map(EntityMapper::toDto)
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<VehiculeReferentielResponse> findAll() {
		return vehiculeReferentielRepository.findAll().stream()
				.map(EntityMapper::toDto)
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public VehiculeReferentielResponse findById(String vehiculeIdRaw) {
		String vehiculeId = normalizeVehiculeId(vehiculeIdRaw);
		return vehiculeReferentielRepository.findById(vehiculeId)
				.map(EntityMapper::toDto)
				.orElseThrow(() -> new BadRequestException(
						"Véhicule inconnu chez G4. Attendre vehicle.registered (G7) ou POST sync-from-g7 : " + vehiculeId));
	}

	public void assertKnownAndDisponibleForAffectation(String vehiculeIdRaw) {
		if (!integrationProperties.isG7FlowEnabled()) {
			return;
		}
		String vehiculeId = normalizeVehiculeId(vehiculeIdRaw);
		VehiculeReferentiel ref = resolveReferentiel(vehiculeId,
				"Véhicule non enregistré chez G4. Créez-le d'abord chez G7 (Kafka vehicle.registered).");
		if (!ref.isDisponiblePourAffectation() || ref.getStatutG7() != StatutVehiculeG7.DISPONIBLE) {
			throw new BadRequestException(
					"Le véhicule " + vehiculeId + " n'est pas DISPONIBLE pour affectation (statut G7="
							+ ref.getStatutG7() + ")");
		}
	}

	public void assertReadyForMission(String vehiculeIdRaw, Long ligneId) {
		if (!integrationProperties.isG7FlowEnabled()) {
			return;
		}
		String vehiculeId = normalizeVehiculeId(vehiculeIdRaw);
		VehiculeReferentiel ref = resolveReferentiel(vehiculeId,
				"Impossible de créer une mission : véhicule inconnu. Affectez d'abord le véhicule à une ligne.");
		if (ref.getStatutG7() != StatutVehiculeG7.EN_SERVICE) {
			throw new BadRequestException(
					"Le véhicule " + vehiculeId + " doit être EN_SERVICE chez G7 (affectation ligne préalable). Statut actuel : "
							+ ref.getStatutG7());
		}
		if (!affectationRepository.existsByVehiculeIdAndLigne_IdAndStatut(vehiculeId, ligneId, StatutAffectation.ACTIF)) {
			throw new BadRequestException(
					"Aucune affectation ACTIF pour le véhicule " + vehiculeId + " sur la ligne " + ligneId);
		}
	}

	@Transactional
	public void markEnServiceAfterAffectation(String vehiculeIdRaw, Long ligneId) {
		String vehiculeId = normalizeVehiculeId(vehiculeIdRaw);
		VehiculeReferentiel ref = vehiculeReferentielRepository.findById(vehiculeId)
				.orElseThrow(() -> new BadRequestException("Référentiel véhicule introuvable : " + vehiculeId));
		ref.setStatutG7(StatutVehiculeG7.EN_SERVICE);
		ref.setDisponiblePourAffectation(false);
		ref.setLigneAffecteeId(ligneId);
		vehiculeReferentielRepository.save(ref);
	}

	/**
	 * Remet le véhicule DISPONIBLE chez G7 et dans le référentiel G4 si plus aucune affectation ACTIF
	 * ni mission EN_COURS sur ce véhicule (contrat G4↔G7 § fin d'affectation).
	 */
	@Transactional
	public void tryReleaseVehicleIfIdle(String vehiculeIdRaw) {
		if (!integrationProperties.isG7FlowEnabled()) {
			return;
		}
		String vehiculeId = normalizeVehiculeId(vehiculeIdRaw);
		if (affectationRepository.existsByVehiculeIdAndStatut(vehiculeId, StatutAffectation.ACTIF)) {
			log.debug("Pas de libération {} : affectation ACTIF encore présente", vehiculeId);
			return;
		}
		if (missionRepository.existsByVehiculeIdAndStatut(vehiculeId, StatutMission.EN_COURS)) {
			log.debug("Pas de libération {} : mission EN_COURS encore présente", vehiculeId);
			return;
		}
		g7VehicleClient.notifyDisponible(vehiculeId);
		markDisponibleAfterRelease(vehiculeId);
		supervisionLogService.add("INFO", "AFFECTATION",
				"Véhicule " + vehiculeId + " libéré → G7 DISPONIBLE");
	}

	@Transactional
	public void markDisponibleAfterRelease(String vehiculeIdRaw) {
		String vehiculeId = normalizeVehiculeId(vehiculeIdRaw);
		vehiculeReferentielRepository.findById(vehiculeId).ifPresent(ref -> {
			ref.setStatutG7(StatutVehiculeG7.DISPONIBLE);
			ref.setDisponiblePourAffectation(true);
			ref.setLigneAffecteeId(null);
			vehiculeReferentielRepository.save(ref);
		});
	}

	public static String normalizeVehiculeId(String vehiculeIdRaw) {
		if (!StringUtils.hasText(vehiculeIdRaw)) {
			throw new BadRequestException("vehiculeId obligatoire");
		}
		String id = vehiculeIdRaw.trim();
		try {
			return UUID.fromString(id).toString();
		} catch (IllegalArgumentException ex) {
			throw new BadRequestException(
					"vehiculeId doit être l'UUID G7 (ex. 53c31262-591a-44d4-8872-51e84611ac5e)");
		}
	}

	public static StatutVehiculeG7 parseStatutG7(String raw, StatutVehiculeG7 defaultStatut) {
		if (!StringUtils.hasText(raw)) {
			return defaultStatut;
		}
		try {
			return StatutVehiculeG7.valueOf(raw.trim().toUpperCase());
		} catch (IllegalArgumentException ex) {
			return StatutVehiculeG7.INCONNU;
		}
	}

	private VehiculeReferentiel resolveReferentiel(String vehiculeId, String detail) {
		return vehiculeReferentielRepository.findById(vehiculeId)
				.orElseGet(() -> trySyncMissingReferentiel(vehiculeId, detail));
	}

	private VehiculeReferentiel trySyncMissingReferentiel(String vehiculeId, String detail) {
		if (integrationProperties.isG7SyncOnDemandEnabled()) {
			try {
				syncFromG7(vehiculeId);
				return vehiculeReferentielRepository.findById(vehiculeId)
						.orElseThrow(() -> new BadRequestException(detail));
			} catch (BadRequestException ex) {
				throw ex;
			} catch (Exception ex) {
				log.warn("Sync G7 échouée pour {}: {}", vehiculeId, ex.getMessage());
			}
		}
		if (integrationProperties.isG7FlowStrict()) {
			throw new BadRequestException(detail);
		}
		throw new BadRequestException(detail);
	}

	private static String trimToNull(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}

	private static String stringOrNull(Object value) {
		return value == null ? null : String.valueOf(value);
	}
}
