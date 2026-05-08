package com.example.taxi.notification.integration;

import com.example.taxi.notification.domain.NotificationRecipientType;
import com.example.taxi.notification.service.NotificationTaskService;
import com.example.taxi.notification.web.dto.CreateNotificationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TripStatusEventListener {

	private static final Logger log = LoggerFactory.getLogger(TripStatusEventListener.class);

	private final NotificationTaskService notificationTaskService;
	private final ObjectMapper objectMapper;

	public TripStatusEventListener(NotificationTaskService notificationTaskService, ObjectMapper objectMapper) {
		this.notificationTaskService = notificationTaskService;
		this.objectMapper = objectMapper;
	}

	@RabbitListener(queues = "${notification.integration.events.queue}")
	public void handle(String payload) {
		TripStatusChangedEvent event;
		try {
			event = objectMapper.readValue(payload, TripStatusChangedEvent.class);
		} catch (Exception ex) {
			log.warn("Failed to parse trip event payload: {}", ex.getMessage());
			return;
		}
		notificationTaskService.createTaskIfNotExists(new CreateNotificationRequest(
			event.tripId(),
			NotificationRecipientType.PASSENGER,
			event.passengerId(),
			"Trip " + event.tripId() + " status changed to " + event.status()
		));
		if (event.driverId() != null) {
			notificationTaskService.createTaskIfNotExists(new CreateNotificationRequest(
				event.tripId(),
				NotificationRecipientType.DRIVER,
				event.driverId(),
				"Trip " + event.tripId() + " status changed to " + event.status()
			));
		}
	}
}
