package com.example.taxi.trip.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record CreateTripRequest(
	@NotNull Long passengerId,
	@NotBlank String origin,
	@NotBlank String destination,
	@NotNull @DecimalMin(value = "0.1") Double distance,
	@NotNull @DecimalMin(value = "0.1") Double tariff
) {
}
