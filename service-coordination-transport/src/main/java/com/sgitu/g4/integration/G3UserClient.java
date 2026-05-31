package com.sgitu.g4.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgitu.g4.config.IntegrationProperties;
import com.sgitu.g4.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Validation optionnelle du {@code chauffeurId} via le contrat G3 :
 * {@code GET /api/users/drivers/ids} (Bearer JWT requis côté G3).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class G3UserClient {

	private final IntegrationProperties integrationProperties;
	private final ObjectMapper objectMapper;

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
			String bearer = currentBearerToken();
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

	private static Long parseDriverId(String chauffeurId) {
		try {
			return Long.parseLong(chauffeurId);
		} catch (NumberFormatException ex) {
			throw new BadRequestException("chauffeurId doit être l'identifiant numérique G3 (ex. 1, 42)");
		}
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
}
