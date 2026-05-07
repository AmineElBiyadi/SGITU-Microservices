package com.sgitu.g4.service;

import com.sgitu.g4.dto.SupervisionLogEntryResponse;
import com.sgitu.g4.entity.AuditLog;
import com.sgitu.g4.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

@Service
public class SupervisionLogService {

	private final Deque<SupervisionLogEntryResponse> buffer = new ConcurrentLinkedDeque<>();
	private final int maxSize;
	private final AuditLogRepository auditLogRepository;

	public SupervisionLogService(@Value("${app.supervision.log-buffer-size:500}") int maxSize,
			AuditLogRepository auditLogRepository) {
		this.maxSize = Math.max(50, maxSize);
		this.auditLogRepository = auditLogRepository;
	}

	public void add(String level, String source, String message) {
		Instant now = Instant.now();
		var entry = SupervisionLogEntryResponse.builder()
				.timestamp(now)
				.level(level)
				.source(source)
				.message(message)
				.build();
		synchronized (buffer) {
			buffer.addLast(entry);
			while (buffer.size() > maxSize) {
				buffer.removeFirst();
			}
		}
		try {
			auditLogRepository.save(AuditLog.builder()
					.timestamp(now)
					.level(level)
					.source(source)
					.message(message)
					.build());
		} catch (DataAccessException ignored) {
			// Continue with in-memory log buffer if DB persistence fails temporarily.
		}
	}

	public List<SupervisionLogEntryResponse> recent() {
		try {
			List<SupervisionLogEntryResponse> persisted = auditLogRepository
					.findAllByOrderByTimestampDesc(PageRequest.of(0, maxSize))
					.stream()
					.map(log -> SupervisionLogEntryResponse.builder()
							.timestamp(log.getTimestamp())
							.level(log.getLevel())
							.source(log.getSource())
							.message(log.getMessage())
							.build())
					.collect(Collectors.toList());
			Collections.reverse(persisted);
			return persisted;
		} catch (DataAccessException ignored) {
			// Fallback for startup/migration windows.
		}
		synchronized (buffer) {
			return new ArrayList<>(buffer);
		}
	}
}
