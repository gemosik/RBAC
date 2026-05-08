package com.example.taxi.trip.integration;

import com.example.taxi.trip.domain.TripStatus;

public record TripStatusChangedEvent(
	Long tripId,
	Long passengerId,
	Long driverId,
	TripStatus status
) {
}
