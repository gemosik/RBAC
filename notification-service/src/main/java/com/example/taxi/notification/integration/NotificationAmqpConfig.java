package com.example.taxi.notification.integration;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationAmqpConfig {

	@Bean
	TopicExchange tripEventsExchange(NotificationEventProperties properties) {
		return new TopicExchange(properties.exchange(), true, false);
	}

	@Bean
	Queue notificationQueue(NotificationEventProperties properties) {
		return new Queue(properties.queue(), true);
	}

	@Bean
	Binding tripStatusBinding(NotificationEventProperties properties, Queue notificationQueue, TopicExchange tripEventsExchange) {
		return BindingBuilder.bind(notificationQueue).to(tripEventsExchange).with(properties.routingKey());
	}
}
