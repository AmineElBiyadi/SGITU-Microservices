package com.sgitu.g4.service.detection;

import com.sgitu.g4.config.DetectionProperties;
import com.sgitu.g4.entity.CoordinationEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class DetectionCooldownService {

	private final DetectionProperties detectionProperties;
	private final Map<String, Instant> lastEmitted = new ConcurrentHashMap<>();

	public boolean canEmit(Long missionId, CoordinationEventType type) {
		String key = missionId + ":" + type.name();
		Instant last = lastEmitted.get(key);
		Instant now = Instant.now();
		if (last != null && last.plusSeconds(detectionProperties.getCooldownMinutes() * 60L).isAfter(now)) {
			return false;
		}
		lastEmitted.put(key, now);
		return true;
	}
}
