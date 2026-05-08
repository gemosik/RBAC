package com.example.taxi.notification;

import com.example.taxi.notification.worker.NotificationProcessingService;
import com.example.taxi.notification.worker.NotificationWorkerManager;
import com.example.taxi.notification.worker.NotificationWorkerProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class NotificationWorkerManagerTests {

	@Test
	void gracefulShutdownDoesNotThrow() {
		NotificationProcessingService processing = Mockito.mock(NotificationProcessingService.class);
		Mockito.when(processing.lockNextPendingTask()).thenReturn(null);
		NotificationWorkerManager manager = new NotificationWorkerManager(
			new NotificationWorkerProperties(1, 10, 10),
			processing
		);
		Assertions.assertDoesNotThrow(manager::shutdown);
	}
}
