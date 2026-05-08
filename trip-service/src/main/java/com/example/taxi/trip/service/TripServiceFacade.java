package com.example.taxi.trip.service;

import com.example.taxi.trip.domain.Trip;
import com.example.taxi.trip.domain.TripIdempotencyKey;
import com.example.taxi.trip.domain.TripStatus;
import com.example.taxi.trip.domain.DriverAvailabilityStatus;
import com.example.taxi.trip.domain.DriverSlot;
import com.example.taxi.trip.integration.TripNotificationPublisher;
import com.example.taxi.trip.repo.DriverSlotRepository;
import com.example.taxi.trip.repo.TripIdempotencyKeyRepository;
import com.example.taxi.trip.repo.TripRepository;
import com.example.taxi.trip.web.dto.CreateTripRequest;
import com.example.taxi.trip.web.dto.TripStatsResponse;
import com.example.taxi.trip.web.dto.TripResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TripServiceFacade {
	private static final int DRIVER_ACCEPT_TIMEOUT_MINUTES = 5;
	private static final int AUTO_RATING_HOURS = 24;
	private static final Map<TripStatus, List<TripStatus>> ALLOWED_TRANSITIONS = Map.of(
		TripStatus.DRIVER_ASSIGNED, List.of(TripStatus.DRIVER_ACCEPTED, TripStatus.CANCELLED),
		TripStatus.DRIVER_ACCEPTED, List.of(TripStatus.IN_PROGRESS, TripStatus.CANCELLED),
		TripStatus.IN_PROGRESS, List.of(TripStatus.COMPLETED, TripStatus.CANCELLED),
		TripStatus.COMPLETED, List.of(),
		TripStatus.CANCELLED, List.of()
	);

	private final TripRepository tripRepository;
	private final DriverSlotRepository driverSlotRepository;
	private final TripIdempotencyKeyRepository idempotencyKeyRepository;
	private final TripNotificationPublisher tripNotificationPublisher;
	private final DriverAvailabilityCacheService driverAvailabilityCacheService;

	public TripServiceFacade(
		TripRepository tripRepository,
		DriverSlotRepository driverSlotRepository,
		TripIdempotencyKeyRepository idempotencyKeyRepository,
		TripNotificationPublisher tripNotificationPublisher,
		DriverAvailabilityCacheService driverAvailabilityCacheService
	) {
		this.tripRepository = tripRepository;
		this.driverSlotRepository = driverSlotRepository;
		this.idempotencyKeyRepository = idempotencyKeyRepository;
		this.tripNotificationPublisher = tripNotificationPublisher;
		this.driverAvailabilityCacheService = driverAvailabilityCacheService;
	}

	public TripResponse createTrip(CreateTripRequest request, String idempotencyKey) {
		if (idempotencyKey != null && !idempotencyKey.isBlank()) {
			TripResponse cached = resolveExistingByIdempotencyKey(idempotencyKey);
			if (cached != null) {
				return cached;
			}
		}
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
		storeIdempotencyKey(idempotencyKey, saved.getId());
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
		validateTransition(trip.getStatus(), status);
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
		if (trip.getRating() != null) {
			throw new BadRequestException("Trip rating already set");
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

	@Scheduled(fixedDelay = 60000)
	public void reassignStaleAcceptedTrips() {
		reassignStaleAcceptedTripsInternal(DRIVER_ACCEPT_TIMEOUT_MINUTES);
	}

	public int reassignStaleAcceptedTripsInternal(int timeoutMinutes) {
		Instant threshold = Instant.now().minusSeconds(timeoutMinutes * 60L);
		List<Trip> staleTrips = tripRepository.findByStatusAndStatusChangedAtBefore(TripStatus.DRIVER_ACCEPTED, threshold);
		int changed = 0;
		for (Trip trip : staleTrips) {
			Long oldDriverId = trip.getDriverId();
			if (oldDriverId != null) {
				driverSlotRepository.findById(oldDriverId).ifPresent(slot -> slot.setStatus(DriverAvailabilityStatus.AVAILABLE));
			}
			DriverSlot replacement = driverSlotRepository.findAvailableForUpdate(PageRequest.of(0, 1)).stream().findFirst().orElse(null);
			if (replacement == null) {
				trip.setDriverId(null);
				trip.setStatus(TripStatus.CANCELLED);
			} else {
				replacement.setStatus(DriverAvailabilityStatus.BUSY);
				trip.setDriverId(replacement.getDriverId());
				trip.setStatus(TripStatus.DRIVER_ASSIGNED);
			}
			driverAvailabilityCacheService.evictAvailableDriversCache();
			tripNotificationPublisher.publishTripStatusChanged(trip);
			changed++;
		}
		return changed;
	}

	@Scheduled(fixedDelay = 3600000)
	public void applyDefaultRatings() {
		applyDefaultRatingsInternal(AUTO_RATING_HOURS, 3);
	}

	public int applyDefaultRatingsInternal(int hours, int defaultRating) {
		Instant threshold = Instant.now().minusSeconds(hours * 3600L);
		List<Trip> trips = tripRepository.findByStatusAndRatingIsNullAndCompletedAtBefore(TripStatus.COMPLETED, threshold);
		for (Trip trip : trips) {
			trip.setRating(defaultRating);
		}
		return trips.size();
	}

	private void validateTransition(TripStatus from, TripStatus to) {
		if (from == to) {
			return;
		}
		List<TripStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(from, List.of());
		if (!allowed.contains(to)) {
			throw new BadRequestException("Invalid status transition: " + from + " -> " + to);
		}
	}

	private TripResponse resolveExistingByIdempotencyKey(String idempotencyKey) {
		return idempotencyKeyRepository.findById(idempotencyKey)
			.flatMap(link -> tripRepository.findById(link.getTripId()))
			.map(this::toResponse)
			.orElse(null);
	}

	private void storeIdempotencyKey(String idempotencyKey, Long tripId) {
		if (idempotencyKey == null || idempotencyKey.isBlank()) {
			return;
		}
		if (idempotencyKeyRepository.existsById(idempotencyKey)) {
			return;
		}
		TripIdempotencyKey link = new TripIdempotencyKey();
		link.setIdempotencyKey(idempotencyKey);
		link.setTripId(tripId);
		link.setCreatedAt(Instant.now());
		idempotencyKeyRepository.save(link);
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
