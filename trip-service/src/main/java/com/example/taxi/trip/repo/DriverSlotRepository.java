package com.example.taxi.trip.repo;

import com.example.taxi.trip.domain.DriverSlot;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

public interface DriverSlotRepository extends JpaRepository<DriverSlot, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select d from DriverSlot d where d.status = com.example.taxi.trip.domain.DriverAvailabilityStatus.AVAILABLE order by d.driverId asc")
	List<DriverSlot> findAvailableForUpdate(Pageable pageable);
}
