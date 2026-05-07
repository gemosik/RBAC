package com.example.taxi.notification.repo;

import com.example.taxi.notification.domain.NotificationTask;
import com.example.taxi.notification.domain.NotificationTaskStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface NotificationTaskRepository extends JpaRepository<NotificationTask, Long> {

	List<NotificationTask> findByTripIdOrderByCreatedAtDesc(Long tripId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select t from NotificationTask t where t.status = :status order by t.createdAt asc")
	List<NotificationTask> findLockedByStatus(NotificationTaskStatus status, Pageable pageable);
}
