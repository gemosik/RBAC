package com.example.taxi.trip.integration;

import com.example.taxi.trip.domain.Trip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class TripNotificationPublisher {

	private static final Logger log = LoggerFactory.getLogger(TripNotificationPublisher.class);

	private final RestTemplate restTemplate;
	private final NotificationClientProperties properties;

	public TripNotificationPublisher(RestTemplate restTemplate, NotificationClientProperties properties) {
		this.restTemplate = restTemplate;
		this.properties = properties;
	}

	public void publishTripStatusChanged(Trip trip) {
		send(new CreateNotificationRequest(
			trip.getId(),
			NotificationRecipientType.PASSENGER,
			trip.getPassengerId(),
			"Trip " + trip.getId() + " status changed to " + trip.getStatus()
		));

		if (trip.getDriverId() != null) {
			send(new CreateNotificationRequest(
				trip.getId(),
				NotificationRecipientType.DRIVER,
				trip.getDriverId(),
				"Trip " + trip.getId() + " status changed to " + trip.getStatus()
			));
		}
	}

	private void send(CreateNotificationRequest request) {
		String url = properties.notificationBaseUrl() + "/notifications";
		try {
			restTemplate.postForEntity(url, request, Void.class);
		} catch (RestClientException ex) {
			log.warn("Notification dispatch failed to {} for trip {}: {}", url, request.tripId(), ex.getMessage());
		}
	}
}
