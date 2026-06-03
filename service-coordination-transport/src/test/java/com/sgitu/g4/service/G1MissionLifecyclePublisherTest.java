package com.sgitu.g4.service;

import com.sgitu.g4.dto.G1MissionLifecycleMessage;
import com.sgitu.g4.entity.Ligne;
import com.sgitu.g4.entity.Mission;
import com.sgitu.g4.entity.StatutMission;
import com.sgitu.g4.entity.Trajet;
import com.sgitu.g4.integration.G1BilletterieClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class G1MissionLifecyclePublisherTest {

	@Mock
	private G1BilletterieClient g1BilletterieClient;

	@Mock
	private SupervisionLogService supervisionLogService;

	@InjectMocks
	private G1MissionLifecyclePublisher publisher;

	@Test
	void publishesDelayAlertOnMissionsLifecycleTopic() {
		Mission mission = sampleMission();

		publisher.publishDelayAlert(mission, 12, "Gare Sud");

		ArgumentCaptor<G1MissionLifecycleMessage> captor = ArgumentCaptor.forClass(G1MissionLifecycleMessage.class);
		verify(g1BilletterieClient).publishMissionLifecycleEvent(eq("M-42"), captor.capture());
		G1MissionLifecycleMessage msg = captor.getValue();
		assertEquals("DELAY_ALERT", msg.getEventType());
		assertEquals("RETARD_SIGNIFICATIF", msg.getMetadata().getReason());
		assertEquals("M-42", msg.getMetadata().getMissionDetails().getMissionId());
		assertEquals("ON_GOING", msg.getMetadata().getMissionDetails().getStatus());
		assertEquals(12, msg.getMetadata().getVariables().get("retardMinutes"));
		assertEquals("Gare Sud", msg.getMetadata().getVariables().get("arret"));
	}

	@Test
	void publishesRouteDeviationOnMissionsLifecycleTopic() {
		Mission mission = sampleMission();

		publisher.publishRouteDeviation(mission, "Secteur Nord");

		ArgumentCaptor<G1MissionLifecycleMessage> captor = ArgumentCaptor.forClass(G1MissionLifecycleMessage.class);
		verify(g1BilletterieClient).publishMissionLifecycleEvent(eq("M-42"), captor.capture());
		G1MissionLifecycleMessage msg = captor.getValue();
		assertEquals("ROUTE_DEVIATION", msg.getEventType());
		assertEquals("DEVIATION_TRAJET", msg.getMetadata().getReason());
		assertEquals("ON_GOING", msg.getMetadata().getMissionDetails().getStatus());
		assertEquals("Secteur Nord", msg.getMetadata().getVariables().get("lieu"));
	}

	private static Mission sampleMission() {
		Ligne ligne = Ligne.builder().id(1L).code("L12").nom("Tanger").build();
		Trajet trajet = Trajet.builder().id(2L).nom("Tetouan").ligne(ligne).build();
		return Mission.builder()
				.id(42L)
				.vehiculeId("00000000-0000-4000-8000-000000000001")
				.ligne(ligne)
				.trajet(trajet)
				.statut(StatutMission.EN_COURS)
				.plannedStart(Instant.parse("2026-05-20T10:00:00Z"))
				.build();
	}
}
