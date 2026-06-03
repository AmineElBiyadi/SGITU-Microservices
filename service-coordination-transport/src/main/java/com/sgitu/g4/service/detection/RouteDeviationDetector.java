package com.sgitu.g4.service.detection;

import com.sgitu.g4.config.DetectionProperties;
import com.sgitu.g4.entity.Arret;
import com.sgitu.g4.entity.Mission;
import com.sgitu.g4.entity.Trajet;
import com.sgitu.g4.entity.TrajetArret;
import com.sgitu.g4.repository.TrajetArretRepository;
import com.sgitu.g4.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RouteDeviationDetector {

	private final TrajetArretRepository trajetArretRepository;
	private final DetectionProperties detectionProperties;

	public Optional<DeviationDetectionResult> detect(Mission mission, BigDecimal lat, BigDecimal longitude) {
		Double pLat = GeoUtils.toDouble(lat);
		Double pLon = GeoUtils.toDouble(longitude);
		if (pLat == null || pLon == null) {
			return Optional.empty();
		}
		Trajet trajet = mission.getTrajet();
		if (trajet == null) {
			return Optional.empty();
		}
		List<TrajetArret> sequence = trajetArretRepository.findByTrajetIdOrderBySequenceOrderAsc(trajet.getId());
		double[][] polyline = toPolyline(sequence);
		if (polyline.length == 0) {
			return Optional.empty();
		}
		double distanceMeters = GeoUtils.minDistanceToPolylineMeters(pLat, pLon, polyline);
		if (distanceMeters <= detectionProperties.getDeviationMaxMeters()) {
			return Optional.empty();
		}
		String details = String.format(
				"Écart %.0f m du tracé officiel (seuil %d m, trajet %s)",
				distanceMeters,
				detectionProperties.getDeviationMaxMeters(),
				trajet.getCode());
		return Optional.of(new DeviationDetectionResult(distanceMeters, details));
	}

	private static double[][] toPolyline(List<TrajetArret> sequence) {
		List<double[]> points = new ArrayList<>();
		for (TrajetArret ta : sequence) {
			Arret arret = ta.getArret();
			if (arret == null) {
				continue;
			}
			Double lat = GeoUtils.toDouble(arret.getLatitude());
			Double lon = GeoUtils.toDouble(arret.getLongitude());
			if (lat != null && lon != null) {
				points.add(new double[] { lat, lon });
			}
		}
		return points.toArray(double[][]::new);
	}
}
