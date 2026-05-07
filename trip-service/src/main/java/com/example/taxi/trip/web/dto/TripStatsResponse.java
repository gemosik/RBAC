package com.example.taxi.trip.web.dto;

import java.time.LocalDate;

public record TripStatsResponse(
	LocalDate date,
	long tripsCount,
	double averagePrice
) {
}
