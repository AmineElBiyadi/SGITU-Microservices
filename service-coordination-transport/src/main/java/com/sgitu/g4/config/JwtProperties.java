package com.sgitu.g4.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "sgitu.jwt")
public class JwtProperties {

	private String secret = "";
	private long expirationMs = 86_400_000L;
}
