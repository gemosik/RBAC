package com.example.taxi.user.web.dto;

import com.example.taxi.user.domain.DriverStatus;
import java.time.Instant;

public record DriverResponse(
	Long id,
	String name,
	String email,
	String phone,
	String licenseNumber,
	DriverStatus status,
	Instant createdAt
) {
}
