package com.example.taxi.trip.web;

import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class SystemController {

	@GetMapping("/ping")
	public Map<String, Object> ping() {
		return Map.of(
			"service", "trip-service",
			"status", "UP",
			"timestamp", Instant.now().toString()
		);
	}
}
