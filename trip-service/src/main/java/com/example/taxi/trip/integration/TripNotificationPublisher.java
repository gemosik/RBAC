package com.example.taxi.trip.integration;

import com.example.taxi.trip.domain.Trip;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TripNotificationPublisher {

	private static final Logger log = LoggerFactory.getLogger(TripNotificationPublisher.class);

	private final RabbitTemplate rabbitTemplate;
	private final TripEventProperties properties;
	private final ObjectMapper objectMapper;

	public TripNotificationPublisher(RabbitTemplate rabbitTemplate, TripEventProperties properties, ObjectMapper objectMapper) {
		this.rabbitTemplate = rabbitTemplate;
		this.properties = properties;
		this.objectMapper = objectMapper;
	}

	public void publishTripStatusChanged(Trip trip) {
		TripStatusChangedEvent event = new TripStatusChangedEvent(
			trip.getId(),
			trip.getPassengerId(),
			trip.getDriverId(),
			trip.getStatus()
		);
		try {
			String payload = objectMapper.writeValueAsString(event);
			rabbitTemplate.convertAndSend(properties.exchange(), properties.routingKey(), payload);
		} catch (JsonProcessingException ex) {
			log.warn("Trip status event serialization failed for trip {}: {}", trip.getId(), ex.getMessage());
		} catch (Exception ex) {
			log.warn("Trip status event publish failed for trip {}: {}", trip.getId(), ex.getMessage());
		}
	}
}
