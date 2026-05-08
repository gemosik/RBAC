package com.example.taxi.trip.web;

import com.example.taxi.trip.service.TripServiceFacade;
import com.example.taxi.trip.web.dto.CreateTripRequest;
import com.example.taxi.trip.web.dto.RateTripRequest;
import com.example.taxi.trip.web.dto.TripStatsResponse;
import com.example.taxi.trip.web.dto.TripResponse;
import com.example.taxi.trip.web.dto.UpdateDriverAvailabilityRequest;
import com.example.taxi.trip.web.dto.UpdateTripStatusRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/trips")
public class TripController {

	private final TripServiceFacade tripServiceFacade;

	public TripController(TripServiceFacade tripServiceFacade) {
		this.tripServiceFacade = tripServiceFacade;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public TripResponse createTrip(
		@Valid @RequestBody CreateTripRequest request,
		@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
	) {
		return tripServiceFacade.createTrip(request, idempotencyKey);
	}

	@GetMapping("/{id}")
	public TripResponse getTrip(@PathVariable Long id) {
		return tripServiceFacade.getTrip(id);
	}

	@GetMapping
	public List<TripResponse> getTripHistory(@RequestParam("passenger_id") Long passengerId) {
		return tripServiceFacade.getTripHistory(passengerId);
	}

	@PatchMapping("/{id}/status")
	public TripResponse updateStatus(
		@PathVariable Long id,
		@Valid @RequestBody UpdateTripStatusRequest request
	) {
		return tripServiceFacade.updateTripStatus(id, request.status());
	}

	@PatchMapping("/{id}/rating")
	public TripResponse rateTrip(
		@PathVariable Long id,
		@Valid @RequestBody RateTripRequest request
	) {
		return tripServiceFacade.rateTrip(id, request.rating());
	}

	@GetMapping("/stats")
	public TripStatsResponse getStats(@RequestParam(value = "date", required = false) LocalDate date) {
		return tripServiceFacade.getStats(date == null ? LocalDate.now() : date);
	}

	@GetMapping("/drivers/available")
	public List<Long> getAvailableDrivers() {
		return tripServiceFacade.getAvailableDriverIds();
	}

	@PatchMapping("/drivers/{driverId}/availability")
	@ResponseStatus(HttpStatus.OK)
	public void updateDriverAvailability(
		@PathVariable Long driverId,
		@Valid @RequestBody UpdateDriverAvailabilityRequest request
	) {
		tripServiceFacade.updateDriverAvailability(driverId, request.status());
	}

	@PostMapping("/maintenance/reassign-stale")
	public int reassignStaleTrips(@RequestParam(defaultValue = "5") int timeoutMinutes) {
		return tripServiceFacade.reassignStaleAcceptedTripsInternal(timeoutMinutes);
	}

	@PostMapping("/maintenance/auto-rate")
	public int autoRateTrips(@RequestParam(defaultValue = "24") int hours, @RequestParam(defaultValue = "3") int rating) {
		return tripServiceFacade.applyDefaultRatingsInternal(hours, rating);
	}
}
