package com.sgitu.g4.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI g4OpenApi() {
		final String scheme = "bearerAuth";
		return new OpenAPI()
				.info(new Info()
						.title("SGITU — G4 Coordination des transports")
						.version("1.0")
						.description("API REST G4 — réseau, flotte, missions, événements de coordination (JWT)."))
				.addSecurityItem(new SecurityRequirement().addList(scheme))
				.components(new Components().addSecuritySchemes(scheme,
						new SecurityScheme()
								.name(scheme)
								.type(SecurityScheme.Type.HTTP)
								.scheme("bearer")
								.bearerFormat("JWT")));
	}
}
