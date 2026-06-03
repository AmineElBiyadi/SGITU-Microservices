package com.sgitu.g4.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GeoUtilsTest {

	@Test
	void pointOnSegment_hasLowDistance() {
		double[][] line = {
				{ 35.0, -5.0 },
				{ 35.01, -5.01 }
		};
		double d = GeoUtils.minDistanceToPolylineMeters(35.005, -5.005, line);
		assertTrue(d < 1000, "point milieu proche du segment");
	}

	@Test
	void pointFarFromSegment_hasLargeDistance() {
		double[][] line = {
				{ 35.0, -5.0 },
				{ 35.01, -5.0 }
		};
		double d = GeoUtils.minDistanceToPolylineMeters(35.5, -5.0, line);
		assertTrue(d > 10_000, "point éloigné du tracé");
	}
}
