package com.example.taxi.trip.service;

import com.example.taxi.trip.domain.Trip;
import com.example.taxi.trip.domain.TripStatus;
import com.example.taxi.trip.domain.DriverAvailabilityStatus;
import com.example.taxi.trip.domain.DriverSlot;
import com.example.taxi.trip.integration.TripNotificationPublisher;
import com.example.taxi.trip.repo.DriverSlotRepository;
import com.example.taxi.trip.repo.TripRepository;
import com.example.taxi.trip.web.dto.CreateTripRequest;
import com.example.taxi.trip.web.dto.TripStatsResponse;
import com.example.taxi.trip.web.dto.TripResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TripServiceFacade {

	private final TripRepository tripRepository;
	private final DriverSlotRepository driverSlotRepository;
	private final TripNotificationPublisher tripNotificationPublisher;
	private final DriverAvailabilityCacheService driverAvailabilityCacheService;

	public TripServiceFacade(
		TripRepository tripRepository,
		DriverSlotRepository driverSlotRepository,
		TripNotificationPublisher tripNotificationPublisher,
		DriverAvailabilityCacheService driverAvailabilityCacheService
	) {
		this.tripRepository = tripRepository;
		this.driverSlotRepository = driverSlotRepository;
		this.tripNotificationPublisher = tripNotificationPublisher;
		this.driverAvailabilityCacheService = driverAvailabilityCacheService;
	}

	public TripResponse createTrip(CreateTripRequest request) {
		if (driverAvailabilityCacheService.getAvailableDriverIds().isEmpty()) {
			throw new ConflictException("No available drivers at the moment");
		}
		DriverSlot slot = driverSlotRepository.findAvailableForUpdate(PageRequest.of(0, 1)).stream()
			.findFirst()
			.orElseThrow(() -> new ConflictException("No available drivers at the moment"));
		slot.setStatus(DriverAvailabilityStatus.BUSY);
		driverAvailabilityCacheService.evictAvailableDriversCache();

		Trip trip = new Trip();
		trip.setPassengerId(request.passengerId());
		trip.setOrigin(request.origin());
		trip.setDestination(request.destination());
		trip.setDistance(request.distance());
		trip.setTariff(request.tariff());
		trip.setDriverId(slot.getDriverId());
		trip.setStatus(TripStatus.DRIVER_ASSIGNED);
		trip.setPrice(request.distance() * request.tariff());

		Trip saved = tripRepository.save(trip);
		tripNotificationPublisher.publishTripStatusChanged(saved);
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
				.ifPresent(slot -> {
					slot.setStatus(DriverAvailabilityStatus.AVAILABLE);
					driverAvailabilityCacheService.evictAvailableDriversCache();
				});
		}
		tripNotificationPublisher.publishTripStatusChanged(trip);
		return toResponse(trip);
	}

	public TripResponse rateTrip(Long id, int rating) {
		Trip trip = tripRepository.findById(id)
			.orElseThrow(() -> new NotFoundException("Trip not found: " + id));
		if (trip.getStatus() != TripStatus.COMPLETED) {
			throw new BadRequestException("Trip can be rated only in COMPLETED status");
		}
		trip.setRating(rating);
		return toResponse(trip);
	}

	@Transactional(readOnly = true)
	public TripStatsResponse getStats(LocalDate date) {
		ZoneId zone = ZoneId.systemDefault();
		Instant from = date.atStartOfDay(zone).toInstant();
		Instant to = date.plusDays(1).atStartOfDay(zone).toInstant();
		long count = tripRepository.countByCreatedAtBetween(from, to);
		double avg = tripRepository.averagePriceForPeriod(from, to);
		return new TripStatsResponse(date, count, avg);
	}

	@Transactional(readOnly = true)
	public List<Long> getAvailableDriverIds() {
		return driverAvailabilityCacheService.getAvailableDriverIds();
	}

	public void updateDriverAvailability(Long driverId, DriverAvailabilityStatus status) {
		DriverSlot slot = driverSlotRepository.findById(driverId).orElseGet(() -> {
			DriverSlot created = new DriverSlot();
			created.setDriverId(driverId);
			return created;
		});
		slot.setStatus(status);
		driverSlotRepository.save(slot);
		driverAvailabilityCacheService.evictAvailableDriversCache();
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
			trip.getDistance(),
			trip.getTariff(),
			trip.getRating(),
			trip.getCreatedAt(),
			trip.getUpdatedAt()
		);
	}
}
