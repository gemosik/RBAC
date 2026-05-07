package com.example.taxi.notification.worker;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification.worker")
public record NotificationWorkerProperties(
	int poolSize,
	long idleDelayMs,
	long processingDelayMs
) {
}
