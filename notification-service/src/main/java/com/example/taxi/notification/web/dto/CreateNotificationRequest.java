package com.example.taxi.notification.web.dto;

import com.example.taxi.notification.domain.NotificationRecipientType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateNotificationRequest(
	@NotNull Long tripId,
	@NotNull NotificationRecipientType recipientType,
	@NotNull Long recipientId,
	@NotBlank String message
) {
}
