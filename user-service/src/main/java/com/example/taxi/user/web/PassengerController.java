package com.example.taxi.user.web;

import com.example.taxi.user.service.UserServiceFacade;
import com.example.taxi.user.web.dto.CreatePassengerRequest;
import com.example.taxi.user.web.dto.PassengerResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/passengers")
public class PassengerController {

	private final UserServiceFacade userServiceFacade;

	public PassengerController(UserServiceFacade userServiceFacade) {
		this.userServiceFacade = userServiceFacade;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public PassengerResponse createPassenger(@Valid @RequestBody CreatePassengerRequest request) {
		return userServiceFacade.createPassenger(request);
	}

	@GetMapping("/{id}")
	public PassengerResponse getPassenger(@PathVariable Long id) {
		return userServiceFacade.getPassenger(id);
	}
}
