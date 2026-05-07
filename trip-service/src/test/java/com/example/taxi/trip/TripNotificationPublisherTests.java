package com.example.taxi.trip;

import com.example.taxi.trip.domain.Trip;
import com.example.taxi.trip.domain.TripStatus;
import com.example.taxi.trip.integration.NotificationClientProperties;
import com.example.taxi.trip.integration.TripNotificationPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.http.HttpStatus.CREATED;

class TripNotificationPublisherTests {

	@Test
	void sendsPassengerAndDriverNotificationsOnStatusChange() {
		RestTemplate restTemplate = new RestTemplate();
		MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
		TripNotificationPublisher publisher = new TripNotificationPublisher(
			restTemplate,
			new NotificationClientProperties("http://notification-service.test")
		);

		server.expect(times(2), requestTo("http://notification-service.test/notifications"))
			.andExpect(method(HttpMethod.POST))
			.andRespond(withStatus(CREATED).contentType(MediaType.APPLICATION_JSON));

		Trip trip = new Trip();
		trip.setPassengerId(11L);
		trip.setDriverId(22L);
		trip.setStatus(TripStatus.IN_PROGRESS);
		setTripId(trip, 99L);

		publisher.publishTripStatusChanged(trip);
		server.verify();
	}

	private void setTripId(Trip trip, Long id) {
		try {
			var field = Trip.class.getDeclaredField("id");
			field.setAccessible(true);
			field.set(trip, id);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}
}
