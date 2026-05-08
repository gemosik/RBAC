package com.example.taxi.trip;

import com.example.taxi.trip.integration.TripEventProperties;
import com.example.taxi.trip.integration.UserClientProperties;
import com.example.taxi.trip.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties({UserClientProperties.class, TripEventProperties.class, JwtProperties.class})
@EnableCaching
@EnableScheduling
public class TripServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(TripServiceApplication.class, args);
	}
}
