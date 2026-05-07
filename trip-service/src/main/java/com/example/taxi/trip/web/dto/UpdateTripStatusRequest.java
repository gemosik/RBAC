package com.example.taxi.trip.web.dto;

import com.example.taxi.trip.domain.TripStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTripStatusRequest(@NotNull TripStatus status) {
}
