package com.example.taxi.notification.integration;

public record TripStatusChangedEvent(
	Long tripId,
	Long passengerId,
	Long driverId,
	String status
) {
}
