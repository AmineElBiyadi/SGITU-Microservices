package com.sgitu.g4.repository;

import com.sgitu.g4.entity.PendingNotification;
import com.sgitu.g4.entity.PendingNotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PendingNotificationRepository extends JpaRepository<PendingNotification, Long> {

	long countByStatus(PendingNotificationStatus status);

	List<PendingNotification> findTop20ByStatusOrderByCreatedAtAsc(PendingNotificationStatus status);
}
