package com.example.taxi.trip.integration;

import com.example.taxi.trip.domain.DriverAvailabilityStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class UserServiceClient {

	private static final Logger log = LoggerFactory.getLogger(UserServiceClient.class);

	private final RestTemplate restTemplate;
	private final UserClientProperties properties;

	public UserServiceClient(RestTemplate restTemplate, UserClientProperties properties) {
		this.restTemplate = restTemplate;
		this.properties = properties;
	}

	public boolean passengerExists(Long passengerId) {
		if (!properties.userSyncEnabled()) {
			return true;
		}
		String url = properties.userBaseUrl() + "/passengers/" + passengerId;
		try {
			ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
			return response.getStatusCode().is2xxSuccessful();
		} catch (RestClientException ex) {
			log.warn("Passenger lookup failed for id {}: {}", passengerId, ex.getMessage());
			return false;
		}
	}

	public void updateDriverStatus(Long driverId, DriverAvailabilityStatus status) {
		if (!properties.userSyncEnabled()) {
			return;
		}
		String url = properties.userBaseUrl() + "/drivers/" + driverId + "/status";
		String mapped = status == DriverAvailabilityStatus.AVAILABLE ? "AVAILABLE" : "BUSY";
		HttpEntity<String> entity = new HttpEntity<>("{\"status\":\"" + mapped + "\"}");
		try {
			restTemplate.exchange(url, HttpMethod.PATCH, entity, String.class);
		} catch (RestClientException ex) {
			log.warn("Driver status sync failed for driver {} -> {}: {}", driverId, mapped, ex.getMessage());
		}
	}
}
