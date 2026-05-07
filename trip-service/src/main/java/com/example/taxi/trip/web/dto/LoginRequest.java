package com.example.taxi.trip.web.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
	@NotBlank String username,
	@NotBlank String password
) {
}
