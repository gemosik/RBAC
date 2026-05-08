package com.example.taxi.notification.integration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification.integration.events")
public record NotificationEventProperties(
	String exchange,
	String queue,
	String routingKey
) {
}
