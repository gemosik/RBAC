package com.example.taxi.trip.service;

import com.example.taxi.trip.domain.DriverAvailabilityStatus;
import com.example.taxi.trip.repo.DriverSlotRepository;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DriverAvailabilityCacheService {

	private final DriverSlotRepository driverSlotRepository;

	public DriverAvailabilityCacheService(DriverSlotRepository driverSlotRepository) {
		this.driverSlotRepository = driverSlotRepository;
	}

	@Transactional(readOnly = true)
	@Cacheable(cacheNames = "availableDrivers", key = "'ids'")
	public List<Long> getAvailableDriverIds() {
		return driverSlotRepository.findByStatusOrderByDriverIdAsc(DriverAvailabilityStatus.AVAILABLE).stream()
			.map(slot -> slot.getDriverId())
			.toList();
	}

	@CacheEvict(cacheNames = "availableDrivers", key = "'ids'")
	public void evictAvailableDriversCache() {
	}
}
