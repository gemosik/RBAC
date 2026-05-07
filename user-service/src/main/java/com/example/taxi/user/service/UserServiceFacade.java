package com.example.taxi.user.service;

import com.example.taxi.user.domain.Driver;
import com.example.taxi.user.domain.Passenger;
import com.example.taxi.user.repo.DriverRepository;
import com.example.taxi.user.repo.PassengerRepository;
import com.example.taxi.user.web.dto.CreateDriverRequest;
import com.example.taxi.user.web.dto.CreatePassengerRequest;
import com.example.taxi.user.web.dto.DriverResponse;
import com.example.taxi.user.web.dto.PassengerResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserServiceFacade {

	private final PassengerRepository passengerRepository;
	private final DriverRepository driverRepository;

	public UserServiceFacade(PassengerRepository passengerRepository, DriverRepository driverRepository) {
		this.passengerRepository = passengerRepository;
		this.driverRepository = driverRepository;
	}

	public PassengerResponse createPassenger(CreatePassengerRequest request) {
		Passenger passenger = new Passenger();
		passenger.setName(request.name());
		passenger.setEmail(request.email());
		passenger.setPhone(request.phone());

		Passenger saved = passengerRepository.save(passenger);
		return toPassengerResponse(saved);
	}

	@Transactional(readOnly = true)
	public PassengerResponse getPassenger(Long id) {
		Passenger passenger = passengerRepository.findById(id)
			.orElseThrow(() -> new NotFoundException("Passenger not found: " + id));
		return toPassengerResponse(passenger);
	}

	public DriverResponse createDriver(CreateDriverRequest request) {
		Driver driver = new Driver();
		driver.setName(request.name());
		driver.setEmail(request.email());
		driver.setPhone(request.phone());
		driver.setLicenseNumber(request.licenseNumber());

		Driver saved = driverRepository.save(driver);
		return toDriverResponse(saved);
	}

	@Transactional(readOnly = true)
	public DriverResponse getDriver(Long id) {
		Driver driver = driverRepository.findById(id)
			.orElseThrow(() -> new NotFoundException("Driver not found: " + id));
		return toDriverResponse(driver);
	}

	public DriverResponse updateDriverStatus(Long id, com.example.taxi.user.domain.DriverStatus status) {
		Driver driver = driverRepository.findById(id)
			.orElseThrow(() -> new NotFoundException("Driver not found: " + id));
		driver.setStatus(status);
		return toDriverResponse(driver);
	}

	private PassengerResponse toPassengerResponse(Passenger passenger) {
		return new PassengerResponse(
			passenger.getId(),
			passenger.getName(),
			passenger.getEmail(),
			passenger.getPhone(),
			passenger.getCreatedAt()
		);
	}

	private DriverResponse toDriverResponse(Driver driver) {
		return new DriverResponse(
			driver.getId(),
			driver.getName(),
			driver.getEmail(),
			driver.getPhone(),
			driver.getLicenseNumber(),
			driver.getStatus(),
			driver.getCreatedAt()
		);
	}
}
