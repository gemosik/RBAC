package com.example.taxi.trip.service;

import com.example.taxi.trip.domain.Trip;
import com.example.taxi.trip.domain.TripStatus;
import com.example.taxi.trip.domain.DriverAvailabilityStatus;
import com.example.taxi.trip.domain.DriverSlot;
import com.example.taxi.trip.repo.DriverSlotRepository;
import com.example.taxi.trip.repo.TripRepository;
import com.example.taxi.trip.web.dto.CreateTripRequest;
import com.example.taxi.trip.web.dto.TripResponse;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TripServiceFacade {

	private static final double DEFAULT_PRICE = 100.0;

	private final TripRepository tripRepository;
	private final DriverSlotRepository driverSlotRepository;

	public TripServiceFacade(TripRepository tripRepository, DriverSlotRepository driverSlotRepository) {
		this.tripRepository = tripRepository;
		this.driverSlotRepository = driverSlotRepository;
	}

	public TripResponse createTrip(CreateTripRequest request) {
		DriverSlot slot = driverSlotRepository.findAvailableForUpdate(PageRequest.of(0, 1)).stream()
			.findFirst()
			.orElseThrow(() -> new ConflictException("No available drivers at the moment"));
		slot.setStatus(DriverAvailabilityStatus.BUSY);

		Trip trip = new Trip();
		trip.setPassengerId(request.passengerId());
		trip.setOrigin(request.origin());
		trip.setDestination(request.destination());
		trip.setDriverId(slot.getDriverId());
		trip.setStatus(TripStatus.DRIVER_ASSIGNED);
		trip.setPrice(DEFAULT_PRICE);

		Trip saved = tripRepository.save(trip);
		return toResponse(saved);
	}

	@Transactional(readOnly = true)
	public TripResponse getTrip(Long id) {
		Trip trip = tripRepository.findById(id)
			.orElseThrow(() -> new NotFoundException("Trip not found: " + id));
		return toResponse(trip);
	}

	@Transactional(readOnly = true)
	public List<TripResponse> getTripHistory(Long passengerId) {
		return tripRepository.findByPassengerIdOrderByCreatedAtDesc(passengerId).stream()
			.map(this::toResponse)
			.toList();
	}

	public TripResponse updateTripStatus(Long id, TripStatus status) {
		Trip trip = tripRepository.findById(id)
			.orElseThrow(() -> new NotFoundException("Trip not found: " + id));
		trip.setStatus(status);
		if ((status == TripStatus.COMPLETED || status == TripStatus.CANCELLED) && trip.getDriverId() != null) {
			driverSlotRepository.findById(trip.getDriverId())
				.ifPresent(slot -> slot.setStatus(DriverAvailabilityStatus.AVAILABLE));
		}
		return toResponse(trip);
	}

	private TripResponse toResponse(Trip trip) {
		return new TripResponse(
			trip.getId(),
			trip.getPassengerId(),
			trip.getDriverId(),
			trip.getStatus(),
			trip.getOrigin(),
			trip.getDestination(),
			trip.getPrice(),
			trip.getCreatedAt(),
			trip.getUpdatedAt()
		);
	}
}
