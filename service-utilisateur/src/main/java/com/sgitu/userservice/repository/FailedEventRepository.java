package com.sgitu.userservice.repository;

import com.sgitu.userservice.entity.EventStatus;
import com.sgitu.userservice.entity.FailedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for the failed_events outbox table.
 */
public interface FailedEventRepository extends JpaRepository<FailedEvent, Long> {

    List<FailedEvent> findByStatusOrderByCreatedAtAsc(EventStatus status);

    long countByStatus(EventStatus status);
}
