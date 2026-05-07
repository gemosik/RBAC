package com.example.taxi.trip.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

	private final JwtProperties properties;
	private final SecretKey key;

	public JwtService(JwtProperties properties) {
		this.properties = properties;
		this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
	}

	public String generateToken(String username, String role) {
		Instant now = Instant.now();
		Instant expiration = now.plusSeconds(properties.expirationSeconds());
		return Jwts.builder()
			.subject(username)
			.claim("role", role)
			.issuedAt(Date.from(now))
			.expiration(Date.from(expiration))
			.signWith(key)
			.compact();
	}

	public Claims parse(String token) {
		return Jwts.parser()
			.verifyWith(key)
			.build()
			.parseSignedClaims(token)
			.getPayload();
	}
}
