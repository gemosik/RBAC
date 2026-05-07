package com.example.taxi.trip.web;

import com.example.taxi.trip.service.TripServiceFacade;
import com.example.taxi.trip.web.dto.CreateTripRequest;
import com.example.taxi.trip.web.dto.TripResponse;
import com.example.taxi.trip.web.dto.UpdateTripStatusRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
	public TripResponse createTrip(@Valid @RequestBody CreateTripRequest request) {
		return tripServiceFacade.createTrip(request);
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
}
