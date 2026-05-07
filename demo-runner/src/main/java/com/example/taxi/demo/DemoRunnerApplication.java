package com.example.taxi.demo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;

public class DemoRunnerApplication {

	private static final String USER_URL = "http://localhost:8081";
	private static final String TRIP_URL = "http://localhost:8082";
	private static final String NOTIFICATION_URL = "http://localhost:8083";

	private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
	private final ObjectMapper mapper = new ObjectMapper();

	public static void main(String[] args) throws Exception {
		new DemoRunnerApplication().run();
	}

	private void run() throws Exception {
		System.out.println("=== Taxi Demo Runner Started ===");

		String token = loginAndGetToken();
		long passengerId = createPassenger();
		createDriverInUserService("Driver One", "d1@test.com", "+70000000011", "LIC-501");
		createDriverInUserService("Driver Two", "d2@test.com", "+70000000012", "LIC-502");

		seedAvailableDriver(501L, token);
		seedAvailableDriver(502L, token);

		long tripId = createTrip(passengerId, token);
		updateTripStatus(tripId, "IN_PROGRESS", token);
		updateTripStatus(tripId, "COMPLETED", token);
		rateTrip(tripId, 5, token);
		readStats(token);
		readNotifications(tripId);

		System.out.println("=== Demo Completed Successfully ===");
	}

	private String loginAndGetToken() throws Exception {
		String body = "{\"username\":\"manager\",\"password\":\"manager123\"}";
		JsonNode node = postJson(TRIP_URL + "/auth/login", body, null, 200);
		String token = node.get("token").asText();
		System.out.println("Token acquired.");
		return token;
	}

	private long createPassenger() throws Exception {
		String body = "{\"name\":\"Demo Passenger\",\"email\":\"passenger@test.com\",\"phone\":\"+70000000001\"}";
		JsonNode node = postJson(USER_URL + "/passengers", body, null, 201);
		long id = node.get("id").asLong();
		System.out.println("Passenger created: id=" + id);
		return id;
	}

	private void createDriverInUserService(String name, String email, String phone, String license) throws Exception {
		String body = String.format(
			"{\"name\":\"%s\",\"email\":\"%s\",\"phone\":\"%s\",\"licenseNumber\":\"%s\"}",
			name, email, phone, license
		);
		postJson(USER_URL + "/drivers", body, null, 201);
		System.out.println("Driver registered in user-service: " + license);
	}

	private void seedAvailableDriver(long driverId, String token) throws Exception {
		String body = "{\"status\":\"AVAILABLE\"}";
		patchJson(TRIP_URL + "/trips/drivers/" + driverId + "/availability", body, token, 200);
		System.out.println("Driver slot seeded in trip-service: driverId=" + driverId);
	}

	private long createTrip(long passengerId, String token) throws Exception {
		String body = String.format(
			"{\"passengerId\":%d,\"origin\":\"Airport\",\"destination\":\"Center\",\"distance\":12.5,\"tariff\":2.4}",
			passengerId
		);
		JsonNode node = postJson(TRIP_URL + "/trips", body, token, 201);
		long tripId = node.get("id").asLong();
		double price = node.get("price").asDouble();
		System.out.println("Trip created: id=" + tripId + ", price=" + price);
		return tripId;
	}

	private void updateTripStatus(long tripId, String status, String token) throws Exception {
		String body = "{\"status\":\"" + status + "\"}";
		patchJson(TRIP_URL + "/trips/" + tripId + "/status", body, token, 200);
		System.out.println("Trip status updated: " + status);
	}

	private void rateTrip(long tripId, int rating, String token) throws Exception {
		String body = "{\"rating\":" + rating + "}";
		patchJson(TRIP_URL + "/trips/" + tripId + "/rating", body, token, 200);
		System.out.println("Trip rated: " + rating + "/5");
	}

	private void readStats(String token) throws Exception {
		String date = LocalDate.now().toString();
		JsonNode node = getJson(TRIP_URL + "/trips/stats?date=" + date, token, 200);
		System.out.println("Stats -> tripsCount=" + node.get("tripsCount").asLong() +
			", averagePrice=" + node.get("averagePrice").asDouble());
	}

	private void readNotifications(long tripId) throws Exception {
		Thread.sleep(600);
		JsonNode node = getJson(NOTIFICATION_URL + "/notifications?trip_id=" + tripId, null, 200);
		System.out.println("Notifications created: " + node.size());
	}

	private JsonNode postJson(String url, String body, String token, int expectedStatus) throws Exception {
		HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
			.timeout(Duration.ofSeconds(8))
			.header("Content-Type", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString(body));
		if (token != null) {
			builder.header("Authorization", "Bearer " + token);
		}
		return execute(builder.build(), expectedStatus);
	}

	private JsonNode patchJson(String url, String body, String token, int expectedStatus) throws Exception {
		HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
			.timeout(Duration.ofSeconds(8))
			.header("Content-Type", "application/json")
			.method("PATCH", HttpRequest.BodyPublishers.ofString(body))
			.header("Authorization", "Bearer " + token);
		return execute(builder.build(), expectedStatus);
	}

	private JsonNode getJson(String url, String token, int expectedStatus) throws Exception {
		HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
			.timeout(Duration.ofSeconds(8))
			.GET();
		if (token != null) {
			builder.header("Authorization", "Bearer " + token);
		}
		return execute(builder.build(), expectedStatus);
	}

	private JsonNode execute(HttpRequest request, int expectedStatus) throws IOException, InterruptedException {
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() != expectedStatus) {
			throw new IllegalStateException("Unexpected status " + response.statusCode() + " body=" + response.body());
		}
		return mapper.readTree(response.body());
	}
}
