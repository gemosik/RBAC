package com.example.taxi.user.web;

import com.example.taxi.user.service.UserServiceFacade;
import com.example.taxi.user.web.dto.CreateDriverRequest;
import com.example.taxi.user.web.dto.DriverResponse;
import com.example.taxi.user.web.dto.UpdateDriverStatusRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/drivers")
public class DriverController {

	private final UserServiceFacade userServiceFacade;

	public DriverController(UserServiceFacade userServiceFacade) {
		this.userServiceFacade = userServiceFacade;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public DriverResponse createDriver(@Valid @RequestBody CreateDriverRequest request) {
		return userServiceFacade.createDriver(request);
	}

	@GetMapping("/{id}")
	public DriverResponse getDriver(@PathVariable Long id) {
		return userServiceFacade.getDriver(id);
	}

	@PatchMapping("/{id}/status")
	public DriverResponse updateDriverStatus(
		@PathVariable Long id,
		@Valid @RequestBody UpdateDriverStatusRequest request
	) {
		return userServiceFacade.updateDriverStatus(id, request.status());
	}
}
