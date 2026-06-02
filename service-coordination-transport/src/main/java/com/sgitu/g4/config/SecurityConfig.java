package com.sgitu.g4.config;

import com.sgitu.g4.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	/**
	 * Rôles alignés sur G3 / JWT G10 — voir {@code docs/ROLES_G3_G4_ALIGNMENT.md}.
	 * <ul>
	 * <li>{@code ROLE_G4_OPERATOR} — gestionnaire réseau (offre transport).</li>
	 * <li>{@code ROLE_DISPATCHER} — gestionnaire flotte (missions, affectations, événements).</li>
	 * <li>{@code ROLE_G4_ADMIN} — supervision et droits complets G4.</li>
	 * </ul>
	 */
	private static final String[] G4_READ = {"G4_OPERATOR", "DISPATCHER", "G4_ADMIN"};
	private static final String[] G4_NETWORK_WRITE = {"G4_OPERATOR", "G4_ADMIN"};
	private static final String[] G4_FLEET_WRITE = {"DISPATCHER", "G4_ADMIN"};
	private static final String[] G4_SUPERVISION = {"G4_ADMIN"};

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
				.csrf(csrf -> csrf.disable())
				.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(
								"/api/auth/login",
								"/api/g4/health",
								"/api/g4/logs",
								"/v3/api-docs/**",
								"/swagger-ui/**",
								"/swagger-ui.html",
								"/swagger-ui/index.html"
						).permitAll()
						.requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
						.requestMatchers(HttpMethod.GET, "/actuator/prometheus", "/actuator/info").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/operator/status").hasAnyRole(G4_SUPERVISION)
						.requestMatchers(HttpMethod.GET, "/api/v1/**").hasAnyRole(G4_READ)
						.requestMatchers(HttpMethod.GET, "/api/g4/**").hasAnyRole(G4_READ)
						.requestMatchers(HttpMethod.POST, "/api/g4/lignes", "/api/g4/lignes/**").hasAnyRole(G4_NETWORK_WRITE)
						.requestMatchers(HttpMethod.PUT, "/api/g4/lignes", "/api/g4/lignes/**").hasAnyRole(G4_NETWORK_WRITE)
						.requestMatchers(HttpMethod.DELETE, "/api/g4/lignes", "/api/g4/lignes/**").hasAnyRole(G4_NETWORK_WRITE)
						.requestMatchers(HttpMethod.POST, "/api/g4/trajets", "/api/g4/trajets/**").hasAnyRole(G4_NETWORK_WRITE)
						.requestMatchers(HttpMethod.PUT, "/api/g4/trajets", "/api/g4/trajets/**").hasAnyRole(G4_NETWORK_WRITE)
						.requestMatchers(HttpMethod.DELETE, "/api/g4/trajets", "/api/g4/trajets/**").hasAnyRole(G4_NETWORK_WRITE)
						.requestMatchers(HttpMethod.POST, "/api/g4/arrets", "/api/g4/arrets/**").hasAnyRole(G4_NETWORK_WRITE)
						.requestMatchers(HttpMethod.PUT, "/api/g4/arrets", "/api/g4/arrets/**").hasAnyRole(G4_NETWORK_WRITE)
						.requestMatchers(HttpMethod.DELETE, "/api/g4/arrets", "/api/g4/arrets/**").hasAnyRole(G4_NETWORK_WRITE)
						.requestMatchers(HttpMethod.POST, "/api/g4/horaires", "/api/g4/horaires/**").hasAnyRole(G4_NETWORK_WRITE)
						.requestMatchers(HttpMethod.PUT, "/api/g4/horaires", "/api/g4/horaires/**").hasAnyRole(G4_NETWORK_WRITE)
						.requestMatchers(HttpMethod.DELETE, "/api/g4/horaires", "/api/g4/horaires/**").hasAnyRole(G4_NETWORK_WRITE)
						.requestMatchers(HttpMethod.POST, "/api/g4/missions", "/api/g4/missions/**").hasAnyRole(G4_FLEET_WRITE)
						.requestMatchers(HttpMethod.PUT, "/api/g4/missions", "/api/g4/missions/**").hasAnyRole(G4_FLEET_WRITE)
						.requestMatchers(HttpMethod.DELETE, "/api/g4/missions", "/api/g4/missions/**").hasAnyRole(G4_FLEET_WRITE)
						.requestMatchers(HttpMethod.POST, "/api/g4/vehicules/sync-from-g7/**").hasAnyRole(G4_FLEET_WRITE)
						.requestMatchers(HttpMethod.POST, "/api/g4/affectations", "/api/g4/affectations/**").hasAnyRole(G4_FLEET_WRITE)
						.requestMatchers(HttpMethod.PUT, "/api/g4/affectations", "/api/g4/affectations/**").hasAnyRole(G4_FLEET_WRITE)
						.requestMatchers(HttpMethod.DELETE, "/api/g4/affectations", "/api/g4/affectations/**").hasAnyRole(G4_FLEET_WRITE)
						.requestMatchers(HttpMethod.POST, "/api/g4/events", "/api/g4/events/**").hasAnyRole(G4_FLEET_WRITE)
						.requestMatchers(HttpMethod.PUT, "/api/g4/events", "/api/g4/events/**").hasAnyRole(G4_FLEET_WRITE)
						.requestMatchers(HttpMethod.DELETE, "/api/g4/events", "/api/g4/events/**").hasAnyRole(G4_FLEET_WRITE)
						.requestMatchers(HttpMethod.POST, "/api/g4/incident-impacts", "/api/g4/incident-impacts/**").hasAnyRole(G4_FLEET_WRITE)
						.requestMatchers(HttpMethod.GET, "/api/g4/incident-impacts", "/api/g4/incident-impacts/**").hasAnyRole(G4_READ)
						.requestMatchers(HttpMethod.GET, "/api/g4/pending-notifications").hasAnyRole(G4_SUPERVISION)
						.requestMatchers(HttpMethod.POST, "/api/g4/pending-notifications", "/api/g4/pending-notifications/**").hasAnyRole(G4_SUPERVISION)
						.requestMatchers(HttpMethod.POST, "/api/notifications/send").hasAnyRole(G4_FLEET_WRITE)
						.anyRequest().authenticated()
				)
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	UserDetailsService userDetailsService(PasswordEncoder encoder) {
		return new InMemoryUserDetailsManager(
				User.withUsername("gestionnaire.reseau").password(encoder.encode("password")).roles("G4_OPERATOR").build(),
				User.withUsername("gestionnaire.flotte").password(encoder.encode("password")).roles("DISPATCHER").build(),
				User.withUsername("admin.technique").password(encoder.encode("password")).roles("G4_ADMIN").build()
		);
	}

	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
		return cfg.getAuthenticationManager();
	}
}
