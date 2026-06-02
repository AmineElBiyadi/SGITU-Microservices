package com.sgitu.g4.util;

import java.math.BigDecimal;

/**
 * Distances géodésiques simplifiées (WGS84) pour détection retard / déviation.
 */
public final class GeoUtils {

	private static final double EARTH_RADIUS_METERS = 6_371_000d;

	private GeoUtils() {
	}

	public static Double toDouble(BigDecimal value) {
		return value == null ? null : value.doubleValue();
	}

	public static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
		double dLat = Math.toRadians(lat2 - lat1);
		double dLon = Math.toRadians(lon2 - lon1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
				+ Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
				* Math.sin(dLon / 2) * Math.sin(dLon / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		return EARTH_RADIUS_METERS * c;
	}

	/**
	 * Distance minimale du point P au segment AB (en mètres).
	 */
	public static double distancePointToSegmentMeters(double pLat, double pLon, double aLat, double aLon, double bLat,
			double bLon) {
		double ab = haversineMeters(aLat, aLon, bLat, bLon);
		if (ab < 1e-6) {
			return haversineMeters(pLat, pLon, aLat, aLon);
		}
		double ap = haversineMeters(aLat, aLon, pLat, pLon);
		double bp = haversineMeters(bLat, bLon, pLat, pLon);
		// Projection approximative sur la droite : si angle obtus aux extrémités, distance au sommet
		if (ap * ap >= ab * ab + bp * bp) {
			return bp;
		}
		if (bp * bp >= ab * ab + ap * ap) {
			return ap;
		}
		// Distance perpendiculaire via aire triangle (formule de Héron)
		double s = (ap + bp + ab) / 2;
		double area = Math.sqrt(Math.max(0, s * (s - ap) * (s - bp) * (s - ab)));
		return 2 * area / ab;
	}

	public static double minDistanceToPolylineMeters(double pLat, double pLon, double[][] points) {
		if (points.length == 0) {
			return Double.MAX_VALUE;
		}
		if (points.length == 1) {
			return haversineMeters(pLat, pLon, points[0][0], points[0][1]);
		}
		double min = Double.MAX_VALUE;
		for (int i = 0; i < points.length - 1; i++) {
			double d = distancePointToSegmentMeters(pLat, pLon,
					points[i][0], points[i][1],
					points[i + 1][0], points[i + 1][1]);
			min = Math.min(min, d);
		}
		return min;
	}
}
