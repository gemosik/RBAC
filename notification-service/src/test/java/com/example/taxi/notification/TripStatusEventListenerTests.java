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
	}
}
