package com.sgitu.g4.util;

import org.springframework.util.StringUtils;

/** Utilitaires de détection (G7 statut panne, G9 incident confirmé). */
public final class CoordinationDetectionUtils {

	private CoordinationDetectionUtils() {
	}

	public static boolean isG7BreakdownStatut(String rawStatut) {
		if (!StringUtils.hasText(rawStatut)) {
			return false;
		}
		String s = rawStatut.trim().toUpperCase();
		return s.contains("PANNE") || s.contains("BROKEN") || "EN_PANNE".equals(s);
	}

	public static boolean isG9IncidentConfirmed(String rawStatut) {
		return G9IncidentStatut.fromRaw(rawStatut) == G9IncidentStatut.CONFIRME;
	}
}
