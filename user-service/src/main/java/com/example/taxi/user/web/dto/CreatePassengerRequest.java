package com.example.taxi.user.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreatePassengerRequest(
	@NotBlank String name,
	@Email @NotBlank String email,
	@NotBlank String phone
) {
}
