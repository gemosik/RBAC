package com.example.taxi.trip;

import com.example.taxi.trip.domain.DriverAvailabilityStatus;
import com.example.taxi.trip.integration.UserClientProperties;
import com.example.taxi.trip.integration.UserServiceClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class UserServiceClientTests {

	@Test
	void checksPassengerAndUpdatesDriverStatus() {
		RestTemplate restTemplate = new RestTemplate();
		MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
		UserServiceClient client = new UserServiceClient(
			restTemplate,
			new UserClientProperties("http://user-service.test", true)
		);

		server.expect(once(), requestTo("http://user-service.test/passengers/10"))
			.andExpect(method(HttpMethod.GET))
			.andRespond(withStatus(OK).contentType(MediaType.APPLICATION_JSON));

		server.expect(once(), requestTo("http://user-service.test/drivers/99/status"))
			.andExpect(method(HttpMethod.PATCH))
			.andExpect(content().json("{\"status\":\"BUSY\"}"))
			.andRespond(withStatus(OK).contentType(MediaType.APPLICATION_JSON));

		boolean exists = client.passengerExists(10L);
		Assertions.assertTrue(exists);
		client.updateDriverStatus(99L, DriverAvailabilityStatus.BUSY);

		server.verify();
	}

	@Test
	void passenger404ReturnsFalse() {
		RestTemplate restTemplate = new RestTemplate();
		MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
		UserServiceClient client = new UserServiceClient(restTemplate, new UserClientProperties("http://user-service.test", true));
		server.expect(once(), requestTo("http://user-service.test/passengers/404"))
			.andRespond(withStatus(NOT_FOUND));
		Assertions.assertFalse(client.passengerExists(404L));
		server.verify();
	}

	@Test
	void passenger500ReturnsFalse() {
		RestTemplate restTemplate = new RestTemplate();
		MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
		UserServiceClient client = new UserServiceClient(restTemplate, new UserClientProperties("http://user-service.test", true));
		server.expect(once(), requestTo("http://user-service.test/passengers/500"))
			.andRespond(withStatus(INTERNAL_SERVER_ERROR));
		Assertions.assertFalse(client.passengerExists(500L));
		server.verify();
	}

	@Test
	void userSyncDisabledSkipsRemoteCalls() {
		RestTemplate restTemplate = new RestTemplate();
		MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
		UserServiceClient client = new UserServiceClient(restTemplate, new UserClientProperties("http://user-service.test", false));
		Assertions.assertTrue(client.passengerExists(1L));
		client.updateDriverStatus(1L, DriverAvailabilityStatus.AVAILABLE);
		server.verify();
	}
}
