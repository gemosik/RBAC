package com.example.taxi.notification.web.dto;

import com.example.taxi.notification.domain.NotificationRecipientType;
import com.example.taxi.notification.domain.NotificationTaskStatus;
import java.time.Instant;

public record NotificationTaskResponse(
	Long id,
	Long tripId,
	NotificationRecipientType recipientType,
	Long recipientId,
	String message,
	NotificationTaskStatus status,
	Integer attempts,
	Instant createdAt,
	Instant updatedAt
) {
}
