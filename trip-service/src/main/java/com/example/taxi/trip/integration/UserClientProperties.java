package com.example.taxi.trip.integration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "trip.integration")
public record UserClientProperties(
	String userBaseUrl,
	boolean userSyncEnabled
) {
}
