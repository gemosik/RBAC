package com.example.taxi.trip.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "driver_slots")
public class DriverSlot {

	@Id
	@Column(name = "driver_id")
	private Long driverId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private DriverAvailabilityStatus status;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@PrePersist
	void onCreate() {
		if (status == null) {
			status = DriverAvailabilityStatus.AVAILABLE;
		}
		updatedAt = Instant.now();
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}

	public Long getDriverId() {
		return driverId;
	}

	public void setDriverId(Long driverId) {
		this.driverId = driverId;
	}

	public DriverAvailabilityStatus getStatus() {
		return status;
	}

	public void setStatus(DriverAvailabilityStatus status) {
		this.status = status;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
