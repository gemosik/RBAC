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
	Double distance,
	Double tariff,
	Integer rating,
	Instant createdAt,
	Instant updatedAt
) {
}
