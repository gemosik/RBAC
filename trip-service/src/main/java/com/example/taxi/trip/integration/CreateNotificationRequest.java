package com.example.taxi.trip.integration;

public record CreateNotificationRequest(
	Long tripId,
	NotificationRecipientType recipientType,
	Long recipientId,
	String message
) {
}
