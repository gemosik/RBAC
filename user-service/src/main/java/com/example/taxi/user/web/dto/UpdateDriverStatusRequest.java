package com.example.taxi.user.web.dto;

import com.example.taxi.user.domain.DriverStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateDriverStatusRequest(@NotNull DriverStatus status) {
}
