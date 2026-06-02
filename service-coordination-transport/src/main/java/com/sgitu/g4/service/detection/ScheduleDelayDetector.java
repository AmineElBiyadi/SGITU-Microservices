package com.sgitu.g4.service.detection;

import com.sgitu.g4.config.DetectionProperties;
import com.sgitu.g4.entity.Arret;
import com.sgitu.g4.entity.Horaire;
import com.sgitu.g4.entity.Mission;
import com.sgitu.g4.entity.Trajet;
import com.sgitu.g4.entity.TrajetArret;
import com.sgitu.g4.repository.HoraireRepository;
import com.sgitu.g4.repository.TrajetArretRepository;
import com.sgitu.g4.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ScheduleDelayDetector {

	private final TrajetArretRepository trajetArretRepository;
	private final HoraireRepository horaireRepository;
	private final DetectionProperties detectionProperties;

	public Optional<DelayDetectionResult> detect(Mission mission, BigDecimal lat, BigDecimal longitude,
			Instant occurredAt) {
		Double pLat = GeoUtils.toDouble(lat);
		Double pLon = GeoUtils.toDouble(longitude);
		if (pLat == null || pLon == null) {
			return Optional.empty();
		}
		Trajet trajet = mission.getTrajet();
		if (trajet == null) {
			return Optional.empty();
		}
		Instant when = occurredAt != null ? occurredAt : Instant.now();
		List<TrajetArret> sequence = trajetArretRepository.findByTrajetIdOrderBySequenceOrderAsc(trajet.getId());
		Optional<NearestStop> nearest = findNearestStopWithinThreshold(sequence, pLat, pLon);
		if (nearest.isEmpty()) {
			return Optional.empty();
		}
		NearestStop stop = nearest.get();
		List<Horaire> horaires = horaireRepository.findByTrajetIdAndArretId(trajet.getId(), stop.arretId());
		if (horaires.isEmpty()) {
			return Optional.empty();
		}
		int dayOfWeek = dayOfWeekIso(when);
		Optional<Horaire> horaireOpt = horaires.stream()
				.filter(h -> h.getJourSemaine() == null || h.getJourSemaine() == dayOfWeek)
				.findFirst();
		if (horaireOpt.isEmpty()) {
			return Optional.empty();
		}
		Horaire horaire = horaireOpt.get();
		LocalDate serviceDate = LocalDate.ofInstant(
				mission.getPlannedStart() != null ? mission.getPlannedStart() : when,
				ZoneOffset.UTC);
		Instant expectedPassage = serviceDate.atTime(horaire.getHeurePassage()).toInstant(ZoneOffset.UTC);
		if (!when.isAfter(expectedPassage)) {
			return Optional.empty();
		}
		long retardMinutes = Duration.between(expectedPassage, when).toMinutes();
		if (retardMinutes <= detectionProperties.getDelayToleranceMinutes()) {
			return Optional.empty();
		}
		String cause = String.format(
				"Retard %d min à l'arrêt %s (prévu %s, seuil %d min)",
				retardMinutes,
				stop.arretNom(),
				horaire.getHeurePassage(),
				detectionProperties.getDelayToleranceMinutes());
		return Optional.of(new DelayDetectionResult((int) retardMinutes, cause, stop.arretNom()));
	}

	private Optional<NearestStop> findNearestStopWithinThreshold(List<TrajetArret> sequence, double pLat, double pLon) {
		return sequence.stream()
				.map(ta -> {
					Arret arret = ta.getArret();
					if (arret == null) {
						return null;
					}
					Double lat = GeoUtils.toDouble(arret.getLatitude());
					Double lon = GeoUtils.toDouble(arret.getLongitude());
					if (lat == null || lon == null) {
						return null;
					}
					double distance = GeoUtils.haversineMeters(pLat, pLon, lat, lon);
					return new NearestStop(arret.getId(), arret.getNom(), distance);
				})
				.filter(s -> s != null)
				.filter(s -> s.distanceMeters() <= detectionProperties.getNearStopMaxMeters())
				.min(Comparator.comparingDouble(NearestStop::distanceMeters));
	}

	private static int dayOfWeekIso(Instant instant) {
		return switch (instant.atZone(ZoneOffset.UTC).getDayOfWeek()) {
			case MONDAY -> 1;
			case TUESDAY -> 2;
			case WEDNESDAY -> 3;
			case THURSDAY -> 4;
			case FRIDAY -> 5;
			case SATURDAY -> 6;
			case SUNDAY -> 7;
		};
	}

	private record NearestStop(Long arretId, String arretNom, double distanceMeters) {
	}
}
