package com.example.taxi.trip.web;

import com.example.taxi.trip.security.JwtService;
import com.example.taxi.trip.web.dto.LoginRequest;
import com.example.taxi.trip.web.dto.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;

	public AuthController(AuthenticationManager authenticationManager, JwtService jwtService) {
		this.authenticationManager = authenticationManager;
		this.jwtService = jwtService;
	}

	@PostMapping("/login")
	public LoginResponse login(@Valid @RequestBody LoginRequest request) {
		Authentication authentication = authenticationManager.authenticate(
			new UsernamePasswordAuthenticationToken(request.username(), request.password())
		);
		String role = authentication.getAuthorities().stream()
			.map(GrantedAuthority::getAuthority)
			.findFirst()
			.map(a -> a.replace("ROLE_", ""))
			.orElse("OPERATOR");
		String token = jwtService.generateToken(authentication.getName(), role);
		return new LoginResponse(token, "Bearer");
	}
}
