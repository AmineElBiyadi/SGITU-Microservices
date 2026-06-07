package com.sgitu.g4.integration;

import com.sgitu.g4.security.ServiceJwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * Promo §3 — appels REST micro → micro : uniquement {@code Authorization: Bearer} (JWT service, secret global).
 * Pas d'en-têtes {@code X-User-*} (réservés au §2 via G10).
 */
@Component
@RequiredArgsConstructor
public class InterServiceHttpAuth {

	public enum Peer {
		G3, G5, G7, G9, GENERIC
	}

	private final ServiceJwtProvider serviceJwtProvider;

	public RestClient.RequestHeadersSpec<?> apply(RestClient.RequestHeadersSpec<?> request) {
		return apply(request, Peer.GENERIC);
	}

	public RestClient.RequestHeadersSpec<?> apply(RestClient.RequestHeadersSpec<?> request, Peer peer) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + resolveOutboundBearer(peer));
	}

	private String resolveOutboundBearer(Peer peer) {
		String fromRequest = currentBearerToken();
		if (peer == Peer.G3 && StringUtils.hasText(fromRequest)) {
			return fromRequest;
		}
		return peer == Peer.G3
				? serviceJwtProvider.createG3Token()
				: serviceJwtProvider.createOutboundToken();
	}

	private static String currentBearerToken() {
		var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || auth.getCredentials() == null) {
			return null;
		}
		String token = auth.getCredentials().toString();
		return StringUtils.hasText(token) ? token : null;
	}
}
