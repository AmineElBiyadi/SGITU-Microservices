package com.sgitu.g4.util;

import org.springframework.util.StringUtils;

/** Statuts G9 attendus sur {@code incident.transport.topic} (contrat G9→G4). */
public enum G9IncidentStatut {
	CONFIRME,
	RESOLU,
	REJETE,
	AUTRE;

	public static G9IncidentStatut fromRaw(String raw) {
		if (!StringUtils.hasText(raw)) {
			return AUTRE;
		}
		String s = raw.trim().toUpperCase();
		if (s.contains("CONFIRM") || "VALIDE".equals(s)) {
			return CONFIRME;
		}
		if (s.contains("RESOL") || s.contains("CLOTUR") || s.contains("TERMINE")) {
			return RESOLU;
		}
		if (s.contains("REJET") || s.contains("REFUS") || s.contains("FAUX")) {
			return REJETE;
		}
		return AUTRE;
	}
}
