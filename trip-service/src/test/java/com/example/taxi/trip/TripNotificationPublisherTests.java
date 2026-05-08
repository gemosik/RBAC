package com.example.taxi.trip;

import com.example.taxi.trip.domain.Trip;
import com.example.taxi.trip.domain.TripStatus;
import com.example.taxi.trip.integration.TripEventProperties;
import com.example.taxi.trip.integration.TripNotificationPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

class TripNotificationPublisherTests {

	@Test
	void publishesTripStatusEventToRabbitMq() {
		RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
		TripNotificationPublisher publisher = new TripNotificationPublisher(
			rabbitTemplate,
			new TripEventProperties("trip.events", "trip.status.changed"),
			new ObjectMapper()
		);

		Trip trip = new Trip();
		trip.setPassengerId(11L);
		trip.setDriverId(22L);
		trip.setStatus(TripStatus.IN_PROGRESS);
		setTripId(trip, 99L);

		publisher.publishTripStatusChanged(trip);

		ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
		verify(rabbitTemplate).convertAndSend(org.mockito.Mockito.eq("trip.events"), org.mockito.Mockito.eq("trip.status.changed"), payloadCaptor.capture());
		Object payload = payloadCaptor.getValue();
		Assertions.assertTrue(payload.toString().contains("99"));
	}

	@Test
	void publishesEventWhenDriverIsNull() {
		RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
		TripNotificationPublisher publisher = new TripNotificationPublisher(
			rabbitTemplate,
			new TripEventProperties("trip.events", "trip.status.changed"),
			new ObjectMapper()
		);
		Trip trip = new Trip();
		trip.setPassengerId(11L);
		trip.setDriverId(null);
		trip.setStatus(TripStatus.CANCELLED);
		setTripId(trip, 101L);

		publisher.publishTripStatusChanged(trip);
		ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
		verify(rabbitTemplate).convertAndSend(org.mockito.Mockito.eq("trip.events"), org.mockito.Mockito.eq("trip.status.changed"), payloadCaptor.capture());
		Assertions.assertTrue(payloadCaptor.getValue().toString().contains("\"driverId\":null"));
	}

	@Test
	void rabbitErrorDoesNotCrashPublisher() {
		RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
		doThrow(new RuntimeException("broker down")).when(rabbitTemplate)
			.convertAndSend(org.mockito.Mockito.anyString(), org.mockito.Mockito.anyString(), org.mockito.Mockito.any(Object.class));
		TripNotificationPublisher publisher = new TripNotificationPublisher(
			rabbitTemplate,
			new TripEventProperties("trip.events", "trip.status.changed"),
			new ObjectMapper()
		);
		Trip trip = new Trip();
		trip.setPassengerId(1L);
		trip.setStatus(TripStatus.DRIVER_ASSIGNED);
		setTripId(trip, 102L);
		Assertions.assertDoesNotThrow(() -> publisher.publishTripStatusChanged(trip));
	}

	private void setTripId(Trip trip, Long id) {
		try {
			var field = Trip.class.getDeclaredField("id");
			field.setAccessible(true);
			field.set(trip, id);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}
}
