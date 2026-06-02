package com.sgitu.g4.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgitu.g4.config.IntegrationProperties;
import com.sgitu.g4.dto.G3NotificationRecipientItem;
import com.sgitu.g4.dto.G3NotificationRecipientsPage;
import com.sgitu.g4.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Intégration G3 : validation {@code chauffeurId} et liste destinataires notifications G5.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class G3UserClient {

	private final IntegrationProperties integrationProperties;
	private final ObjectMapper objectMapper;

	public record NotificationRecipient(String userId, String email) {
	}

	public void assertDriverExistsIfEnabled(String chauffeurId) {
		if (!StringUtils.hasText(chauffeurId) || !integrationProperties.isG3ValidationEnabled()) {
			return;
		}
		String id = chauffeurId.trim();
		Long driverId = parseDriverId(id);
		try {
			RestClient.RequestHeadersSpec<?> request = RestClient.create(integrationProperties.getG3BaseUrl())
					.get()
					.uri(integrationProperties.getG3DriversIdsPath());
			String bearer = resolveBearerToken();
			if (StringUtils.hasText(bearer)) {
				request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + bearer);
			}
			String body = request.retrieve().body(String.class);
			if (body == null) {
				handleUnavailable("Réponse vide G3 (drivers/ids)");
				return;
			}
			List<Long> driverIds = objectMapper.readValue(body, new TypeReference<>() {});
			if (driverIds == null || !driverIds.contains(driverId)) {
				throw new BadRequestException(
						"Conducteur introuvable dans G3 (chauffeurId=" + id
								+ "). IDs chauffeurs G3 disponibles : " + driverIds);
			}
		} catch (BadRequestException ex) {
			throw ex;
		} catch (Exception ex) {
			log.warn("Validation G3 indisponible pour chauffeurId={}: {}", id, ex.getMessage());
			handleUnavailable("G3 injoignable ou JWT refusé : " + ex.getMessage());
		}
	}

	/**
	 * Liste paginée des destinataires pour G5 ({@code userId} + {@code email}, comptes actifs côté G3).
	 * Retourne une liste vide si l'appel G3 est désactivé ou en échec (mode non strict).
	 */
	public List<NotificationRecipient> listNotificationRecipients() {
		if (!integrationProperties.isG3NotificationRecipientsEnabled()) {
			return Collections.emptyList();
		}
		List<NotificationRecipient> all = new ArrayList<>();
		int page = 0;
		int size = Math.max(1, integrationProperties.getG3NotificationRecipientsPageSize());
		try {
			while (true) {
				G3NotificationRecipientsPage chunk = fetchRecipientsPage(page, size);
				if (chunk.getItems() == null || chunk.getItems().isEmpty()) {
					break;
				}
				for (G3NotificationRecipientItem item : chunk.getItems()) {
					if (item == null || item.getUserId() == null || !StringUtils.hasText(item.getEmail())) {
						continue;
					}
					all.add(new NotificationRecipient(
							String.valueOf(item.getUserId()),
							item.getEmail().trim()));
				}
				long total = chunk.getTotal() > 0 ? chunk.getTotal() : all.size();
				page++;
				if ((long) page * size >= total) {
					break;
				}
			}
		} catch (Exception ex) {
			log.warn("Liste destinataires G3 indisponible: {}", ex.getMessage());
			handleRecipientsUnavailable(ex.getMessage());
		}
		return all;
	}

	private G3NotificationRecipientsPage fetchRecipientsPage(int page, int size) throws com.fasterxml.jackson.core.JsonProcessingException {
		UriComponentsBuilder uri = UriComponentsBuilder
				.fromUriString(integrationProperties.getG3BaseUrl()
						+ integrationProperties.getG3NotificationRecipientsPath())
				.queryParam("page", page)
				.queryParam("size", size);
		if (StringUtils.hasText(integrationProperties.getG3NotificationRecipientRoles())) {
			uri.queryParam("roles", integrationProperties.getG3NotificationRecipientRoles());
		}
		RestClient.RequestHeadersSpec<?> request = RestClient.create().get().uri(uri.build(true).toUri());
		String bearer = resolveBearerToken();
		if (StringUtils.hasText(bearer)) {
			request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + bearer);
		}
		String body = request.retrieve().body(String.class);
		if (body == null || body.isBlank()) {
			return new G3NotificationRecipientsPage();
		}
		return objectMapper.readValue(body, G3NotificationRecipientsPage.class);
	}

	private static Long parseDriverId(String chauffeurId) {
		try {
			return Long.parseLong(chauffeurId);
		} catch (NumberFormatException ex) {
			throw new BadRequestException("chauffeurId doit être l'identifiant numérique G3 (ex. 1, 42)");
		}
	}

	/** JWT de la requête HTTP en cours, sinon token service G4 (alertes Kafka / batch). */
	private String resolveBearerToken() {
		String fromRequest = currentBearerToken();
		if (StringUtils.hasText(fromRequest)) {
			return fromRequest;
		}
		String serviceToken = integrationProperties.getG3ServiceBearerToken();
		return StringUtils.hasText(serviceToken) ? serviceToken.trim() : null;
	}

	private static String currentBearerToken() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || auth.getCredentials() == null) {
			return null;
		}
		String token = auth.getCredentials().toString();
		return StringUtils.hasText(token) ? token : null;
	}

	private void handleUnavailable(String detail) {
		if (integrationProperties.isG3ValidationStrict()) {
			throw new BadRequestException("Validation conducteur G3 requise mais impossible : " + detail);
		}
		log.info("Validation G3 ignorée (mode non strict) : {}", detail);
	}

	private void handleRecipientsUnavailable(String detail) {
		if (integrationProperties.isG3NotificationRecipientsStrict()) {
			throw new BadRequestException("Liste destinataires G3 requise mais impossible : " + detail);
		}
		log.info("Destinataires G3 ignorés (mode non strict) : {}", detail);
	}
}
