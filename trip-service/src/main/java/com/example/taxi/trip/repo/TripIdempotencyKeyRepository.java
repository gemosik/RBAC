package com.example.taxi.trip.repo;

import com.example.taxi.trip.domain.TripIdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripIdempotencyKeyRepository extends JpaRepository<TripIdempotencyKey, String> {
}
