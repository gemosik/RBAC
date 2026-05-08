package com.example.taxi.trip.integration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "trip.integration.events")
public record TripEventProperties(
	String exchange,
	String routingKey
) {
}
