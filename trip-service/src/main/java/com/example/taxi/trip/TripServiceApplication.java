package com.example.taxi.trip;

import com.example.taxi.trip.integration.NotificationClientProperties;
import com.example.taxi.trip.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableConfigurationProperties({NotificationClientProperties.class, JwtProperties.class})
@EnableCaching
public class TripServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(TripServiceApplication.class, args);
	}
}
