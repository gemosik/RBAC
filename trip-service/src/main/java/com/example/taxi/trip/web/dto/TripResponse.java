package com.example.taxi.trip.web.dto;

import com.example.taxi.trip.domain.TripStatus;
import java.time.Instant;

public record TripResponse(
	Long id,
	Long passengerId,
	Long driverId,
	TripStatus status,
	String origin,
	String destination,
	Double price,
	Instant createdAt,
	Instant updatedAt
) {
}
