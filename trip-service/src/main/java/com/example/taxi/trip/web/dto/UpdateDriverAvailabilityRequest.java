package com.example.taxi.trip.web.dto;

import com.example.taxi.trip.domain.DriverAvailabilityStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateDriverAvailabilityRequest(@NotNull DriverAvailabilityStatus status) {
}
