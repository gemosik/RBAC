package com.example.taxi.notification.service;

import com.example.taxi.notification.domain.NotificationTask;
import com.example.taxi.notification.domain.NotificationTaskStatus;
import com.example.taxi.notification.repo.NotificationTaskRepository;
import com.example.taxi.notification.web.dto.CreateNotificationRequest;
import com.example.taxi.notification.web.dto.NotificationTaskResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationTaskService {

	private final NotificationTaskRepository notificationTaskRepository;

	public NotificationTaskService(NotificationTaskRepository notificationTaskRepository) {
		this.notificationTaskRepository = notificationTaskRepository;
	}

	public NotificationTaskResponse createTask(CreateNotificationRequest request) {
		NotificationTask task = new NotificationTask();
		task.setTripId(request.tripId());
		task.setRecipientType(request.recipientType());
		task.setRecipientId(request.recipientId());
		task.setMessage(request.message());
		task.setStatus(NotificationTaskStatus.PENDING);
		task.setAttempts(0);
		return toResponse(notificationTaskRepository.save(task));
	}

	public NotificationTaskResponse createTaskIfNotExists(CreateNotificationRequest request) {
		boolean exists = notificationTaskRepository.existsByTripIdAndRecipientTypeAndRecipientIdAndMessage(
			request.tripId(), request.recipientType(), request.recipientId(), request.message()
		);
		if (exists) {
			return null;
		}
		return createTask(request);
	}

	@Transactional(readOnly = true)
	public List<NotificationTaskResponse> getByTripId(Long tripId) {
		return notificationTaskRepository.findByTripIdOrderByCreatedAtDesc(tripId).stream()
			.map(this::toResponse)
			.toList();
	}

	NotificationTaskResponse toResponse(NotificationTask task) {
		return new NotificationTaskResponse(
			task.getId(),
			task.getTripId(),
			task.getRecipientType(),
			task.getRecipientId(),
			task.getMessage(),
			task.getStatus(),
			task.getAttempts(),
			task.getCreatedAt(),
			task.getUpdatedAt()
		);
	}
}
