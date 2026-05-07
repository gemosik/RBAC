package com.example.taxi.trip.repo;

import com.example.taxi.trip.domain.Trip;
import java.util.List;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TripRepository extends JpaRepository<Trip, Long> {
	List<Trip> findByPassengerIdOrderByCreatedAtDesc(Long passengerId);

	long countByCreatedAtBetween(Instant fromInclusive, Instant toExclusive);

	@Query("select coalesce(avg(t.price), 0) from Trip t where t.createdAt >= :fromInclusive and t.createdAt < :toExclusive")
	Double averagePriceForPeriod(Instant fromInclusive, Instant toExclusive);
}
