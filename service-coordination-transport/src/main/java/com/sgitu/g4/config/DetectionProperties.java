package com.sgitu.g4.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "sgitu.detection")
public class DetectionProperties {

	private boolean enabled = true;
	private int deviationMaxMeters = 150;
	private int delayToleranceMinutes = 5;
	private int nearStopMaxMeters = 80;
	private int cooldownMinutes = 10;
}
