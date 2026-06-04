package com.sgitu.g4.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class G9IncidentStatutTest {

	@Test
	void fromRaw_reconnaitLesTroisStatutsContrat() {
		assertEquals(G9IncidentStatut.CONFIRME, G9IncidentStatut.fromRaw("CONFIRME"));
		assertEquals(G9IncidentStatut.RESOLU, G9IncidentStatut.fromRaw("RESOLU"));
		assertEquals(G9IncidentStatut.REJETE, G9IncidentStatut.fromRaw("REJETE"));
	}

	@Test
	void isG9IncidentConfirmed_uniquementConfirme() {
		assertTrue(CoordinationDetectionUtils.isG9IncidentConfirmed("CONFIRME"));
		assertFalse(CoordinationDetectionUtils.isG9IncidentConfirmed("RESOLU"));
		assertFalse(CoordinationDetectionUtils.isG9IncidentConfirmed("REJETE"));
	}
}
