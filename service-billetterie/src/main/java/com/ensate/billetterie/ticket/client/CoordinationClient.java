package com.ensate.billetterie.ticket.client;

import com.ensate.billetterie.ticket.dto.response.MissionDTO;
import com.ensate.billetterie.validation.exceptions.ValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Client for calling the Coordination microservice using RestTemplate.
 * This client retrieves mission data from the Coordination Service
 * without storing mission data locally in the Billetterie microservice.
 *
 * This follows the microservices principle: each service owns its data.
 */
@Component
public class CoordinationClient {

    private final RestTemplate restTemplate;
    private final String coordinationServiceUrl;

    // ✅ RestTemplate injecté par Spring (déclaré dans RestTemplateConfig)
    //    → testable facilement (mock), centralisé, géré par le conteneur
    public CoordinationClient(RestTemplate restTemplate,
                               @Value("${coordination-service.url}") String coordinationServiceUrl) {
        this.restTemplate = restTemplate;
        this.coordinationServiceUrl = coordinationServiceUrl;
    }

    /**
     * Retrieve a mission by its ID from the Coordination microservice.
     *
     * Fail-closed strategy: if the Coordination Service is unreachable,
     * ticket validation is blocked (safer than letting anyone through).
     *
     * @param missionId the unique identifier of the mission
     * @return MissionDTO containing mission details
     * @throws ValidationException if the mission is not found or the service is unavailable
     */
    public MissionDTO getMission(String missionId) {
        String url = UriComponentsBuilder.fromHttpUrl(coordinationServiceUrl)
                .pathSegment("missions", missionId)
                .toUriString();

        try {
            return restTemplate.getForObject(url, MissionDTO.class);

        } catch (HttpClientErrorException.NotFound ex) {
            // ✅ 404 → la mission n'existe pas : erreur métier claire
            throw new ValidationException(
                    "EventActiveStep",
                    "Mission introuvable : " + missionId
            );

        } catch (RestClientException ex) {
            // ✅ Réseau down, timeout, 5xx, etc. → stratégie fail-closed :
            //    on bloque la validation plutôt que de laisser passer n'importe qui
            throw new ValidationException(
                    "EventActiveStep",
                    "Service de coordination indisponible — validation bloquée par sécurité"
            );
        }
    }
}

