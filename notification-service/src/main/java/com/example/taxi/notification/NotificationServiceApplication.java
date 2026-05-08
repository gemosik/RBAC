package com.example.taxi.notification;

import com.example.taxi.notification.integration.NotificationEventProperties;
import com.example.taxi.notification.worker.NotificationWorkerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({NotificationWorkerProperties.class, NotificationEventProperties.class})
public class NotificationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(NotificationServiceApplication.class, args);
	}
}
