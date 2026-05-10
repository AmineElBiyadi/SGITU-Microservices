package com.sgitu.g4.repository;

import com.sgitu.g4.entity.CoordinationEventEntity;
import com.sgitu.g4.entity.CoordinationEventStatus;
import com.sgitu.g4.entity.CoordinationEventType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CoordinationEventRepository extends JpaRepository<CoordinationEventEntity, Long> {

	List<CoordinationEventEntity> findByTypeOrderByOccurredAtDesc(CoordinationEventType type);

	List<CoordinationEventEntity> findByStatusOrderByOccurredAtDesc(CoordinationEventStatus status);

	List<CoordinationEventEntity> findAllByOrderByOccurredAtDesc();
}
