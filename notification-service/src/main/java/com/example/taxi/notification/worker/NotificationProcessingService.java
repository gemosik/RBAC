package com.example.taxi.notification.worker;

import com.example.taxi.notification.domain.NotificationTask;
import com.example.taxi.notification.domain.NotificationTaskStatus;
import com.example.taxi.notification.repo.NotificationTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationProcessingService {

	private static final Logger log = LoggerFactory.getLogger(NotificationProcessingService.class);
	private static final int MAX_ATTEMPTS = 3;

	private final NotificationTaskRepository repository;
	private final NotificationWorkerProperties properties;

	public NotificationProcessingService(NotificationTaskRepository repository, NotificationWorkerProperties properties) {
		this.repository = repository;
		this.properties = properties;
	}

	@Transactional
	public Long lockNextPendingTask() {
		NotificationTask task = repository.findLockedByStatus(NotificationTaskStatus.PENDING, PageRequest.of(0, 1))
			.stream()
			.findFirst()
			.orElse(null);
		if (task == null) {
			return null;
		}
		task.setStatus(NotificationTaskStatus.IN_PROGRESS);
		return task.getId();
	}

	@Transactional
	public void processLockedTask(Long taskId, String workerName) {
		NotificationTask task = repository.findById(taskId).orElse(null);
		if (task == null || task.getStatus() != NotificationTaskStatus.IN_PROGRESS) {
			return;
		}

		try {
			simulateSend(task, workerName);
			task.setStatus(NotificationTaskStatus.SENT);
		} catch (RuntimeException ex) {
			int nextAttempt = task.getAttempts() + 1;
			task.setAttempts(nextAttempt);
			task.setStatus(nextAttempt >= MAX_ATTEMPTS ? NotificationTaskStatus.FAILED : NotificationTaskStatus.PENDING);
			log.warn("Worker {} failed to process task {} (attempt {}): {}", workerName, taskId, nextAttempt, ex.getMessage());
		}
	}

	private void simulateSend(NotificationTask task, String workerName) {
		// Educational deterministic failure trigger to demo retries quickly.
		if (task.getMessage().contains("[FAIL]")) {
			throw new IllegalStateException("Simulated send failure");
		}
		try {
			Thread.sleep(properties.processingDelayMs());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		log.info("Worker {} sent notification task {} to {} {} (trip={})",
			workerName, task.getId(), task.getRecipientType(), task.getRecipientId(), task.getTripId());
	}
}
