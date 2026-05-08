package com.example.taxi.notification;

import com.example.taxi.notification.integration.TripStatusEventListener;
import com.example.taxi.notification.repo.NotificationTaskRepository;
import com.example.taxi.notification.service.NotificationTaskService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TripStatusEventListenerTests {

	@Autowired
	private TripStatusEventListener listener;

	@Autowired
	private NotificationTaskRepository repository;

	@Autowired
	private NotificationTaskService service;

	@Test
	void createsPassengerAndDriverTasksFromEvent() {
		repository.deleteAll();
		listener.handle("{\"tripId\":123,\"passengerId\":10,\"driverId\":20,\"status\":\"IN_PROGRESS\"}");
		var tasks = service.getByTripId(123L);
		Assertions.assertEquals(2, tasks.size());
		Assertions.assertTrue(tasks.stream().anyMatch(t -> t.recipientType().name().equals("PASSENGER")));
		Assertions.assertTrue(tasks.stream().anyMatch(t -> t.recipientType().name().equals("DRIVER")));
		Assertions.assertTrue(tasks.stream().allMatch(t -> t.message().contains("status changed")));
		Assertions.assertTrue(tasks.stream().allMatch(t -> t.status().name().equals("PENDING")));
	}

	@Test
	void createsOnlyPassengerTaskWhenDriverIsNull() {
		repository.deleteAll();
		listener.handle("{\"tripId\":124,\"passengerId\":11,\"driverId\":null,\"status\":\"CANCELLED\"}");
		var tasks = service.getByTripId(124L);
		Assertions.assertEquals(1, tasks.size());
		Assertions.assertEquals("PASSENGER", tasks.get(0).recipientType().name());
	}

	@Test
	void duplicateEventIsIdempotent() {
		repository.deleteAll();
		String payload = "{\"tripId\":125,\"passengerId\":12,\"driverId\":21,\"status\":\"IN_PROGRESS\"}";
		listener.handle(payload);
		listener.handle(payload);
		var tasks = service.getByTripId(125L);
		Assertions.assertEquals(2, tasks.size());
	}
}
