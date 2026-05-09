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

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
				.csrf(csrf -> csrf.disable())
				.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(
								"/api/auth/login",
								"/api/g4/health",
								"/v3/api-docs/**",
								"/swagger-ui/**",
								"/swagger-ui.html",
								"/swagger-ui/index.html"
						).permitAll()
						.requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/g4/**", "/api/v1/operator/status")
						.hasAnyRole("G4_OPERATOR", "G4_DISPATCHER", "G4_ADMIN")
						.requestMatchers(HttpMethod.POST, "/api/g4/**", "/api/notifications/send")
						.hasAnyRole("G4_DISPATCHER", "G4_ADMIN")
						.requestMatchers(HttpMethod.PUT, "/api/g4/**")
						.hasAnyRole("G4_DISPATCHER", "G4_ADMIN")
						.requestMatchers(HttpMethod.DELETE, "/api/g4/**")
						.hasAnyRole("G4_DISPATCHER", "G4_ADMIN")
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
				User.withUsername("gestionnaire.reseau").password(encoder.encode("password")).roles("G4_DISPATCHER").build(),
				User.withUsername("gestionnaire.flotte").password(encoder.encode("password")).roles("G4_DISPATCHER").build(),
				User.withUsername("admin.technique").password(encoder.encode("password")).roles("G4_ADMIN").build(),
				User.withUsername("operateur").password(encoder.encode("password")).roles("G4_OPERATOR").build(),
				User.withUsername("g10.integration").password(encoder.encode("password"))
						.roles("GATEWAY_G10", "G4_ADMIN").build()
		);
	}

	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
		return cfg.getAuthenticationManager();
	}
}
