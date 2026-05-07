package com.example.taxi.trip.repo;

import com.example.taxi.trip.domain.Trip;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripRepository extends JpaRepository<Trip, Long> {
	List<Trip> findByPassengerIdOrderByCreatedAtDesc(Long passengerId);
}
