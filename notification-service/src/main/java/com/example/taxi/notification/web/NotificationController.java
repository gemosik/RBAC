package com.example.taxi.notification.web;

import com.example.taxi.notification.service.NotificationTaskService;
import com.example.taxi.notification.web.dto.CreateNotificationRequest;
import com.example.taxi.notification.web.dto.NotificationTaskResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

	private final NotificationTaskService notificationTaskService;

	public NotificationController(NotificationTaskService notificationTaskService) {
		this.notificationTaskService = notificationTaskService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public NotificationTaskResponse create(@Valid @RequestBody CreateNotificationRequest request) {
		return notificationTaskService.createTask(request);
	}

	@GetMapping
	public List<NotificationTaskResponse> getByTripId(@RequestParam("trip_id") Long tripId) {
		return notificationTaskService.getByTripId(tripId);
	}
}
