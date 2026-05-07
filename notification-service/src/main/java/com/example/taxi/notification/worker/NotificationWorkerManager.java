package com.example.taxi.notification.worker;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NotificationWorkerManager {

	private static final Logger log = LoggerFactory.getLogger(NotificationWorkerManager.class);

	private final NotificationWorkerProperties properties;
	private final NotificationProcessingService processingService;
	private final ExecutorService executor;
	private volatile boolean running;

	public NotificationWorkerManager(
		NotificationWorkerProperties properties,
		NotificationProcessingService processingService
	) {
		this.properties = properties;
		this.processingService = processingService;
		this.executor = Executors.newFixedThreadPool(properties.poolSize());
		start();
	}

	private void start() {
		running = true;
		for (int i = 0; i < properties.poolSize(); i++) {
			final String workerName = "notif-worker-" + (i + 1);
			executor.submit(() -> workerLoop(workerName));
		}
		log.info("Started notification worker pool with {} threads", properties.poolSize());
	}

	private void workerLoop(String workerName) {
		while (running && !Thread.currentThread().isInterrupted()) {
			try {
				Long taskId = processingService.lockNextPendingTask();
				if (taskId == null) {
					Thread.sleep(properties.idleDelayMs());
					continue;
				}
				processingService.processLockedTask(taskId, workerName);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			} catch (Exception ex) {
				log.error("Worker {} crashed loop iteration: {}", workerName, ex.getMessage(), ex);
			}
		}
	}

	@PreDestroy
	public void shutdown() {
		running = false;
		executor.shutdown();
		try {
			if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
				executor.shutdownNow();
			}
		} catch (InterruptedException e) {
			executor.shutdownNow();
			Thread.currentThread().interrupt();
		}
		log.info("Notification workers stopped gracefully");
	}
}
