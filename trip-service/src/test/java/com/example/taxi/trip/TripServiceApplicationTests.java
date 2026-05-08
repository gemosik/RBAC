package com.example.taxi.trip;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.taxi.trip.domain.DriverAvailabilityStatus;
import com.example.taxi.trip.domain.DriverSlot;
import com.example.taxi.trip.repo.DriverSlotRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class TripServiceApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private DriverSlotRepository driverSlotRepository;

	private String authHeaderValue;

	@BeforeEach
	void seedDrivers() throws Exception {
		driverSlotRepository.deleteAll();
		driverSlotRepository.save(driverSlot(1L, DriverAvailabilityStatus.AVAILABLE));
		driverSlotRepository.save(driverSlot(2L, DriverAvailabilityStatus.AVAILABLE));
		authHeaderValue = "Bearer " + issueToken();
	}

	@Test
	void scenario1_idempotencyDoubleClickCreatesSingleTrip() throws Exception {
		String payload = tripPayload(100, 12.5, 2.0);
		String key = "req-abc-001";

		MvcResult first = mockMvc.perform(post("/trips")
				.header(HttpHeaders.AUTHORIZATION, authHeaderValue)
				.header("Idempotency-Key", key)
				.contentType(MediaType.APPLICATION_JSON)
				.content(payload))
			.andExpect(status().isCreated())
			.andReturn();

		MvcResult second = mockMvc.perform(post("/trips")
				.header(HttpHeaders.AUTHORIZATION, authHeaderValue)
				.header("Idempotency-Key", key)
				.contentType(MediaType.APPLICATION_JSON)
				.content(payload))
			.andExpect(status().isCreated())
			.andReturn();

		Integer id1 = (Integer) objectMapper.readValue(first.getResponse().getContentAsString(), Map.class).get("id");
		Integer id2 = (Integer) objectMapper.readValue(second.getResponse().getContentAsString(), Map.class).get("id");
		org.junit.jupiter.api.Assertions.assertEquals(id1, id2);
	}

	@Test
	void scenario2_driverAcceptedButNotStartedReassigned() throws Exception {
		Integer tripId = createTripAndGetId(500, 8.0, 3.0, null);
		mockMvc.perform(patch("/trips/{id}/status", tripId)
				.header(HttpHeaders.AUTHORIZATION, authHeaderValue)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"DRIVER_ACCEPTED\"}"))
			.andExpect(status().isOk());

		mockMvc.perform(post("/trips/maintenance/reassign-stale")
				.header(HttpHeaders.AUTHORIZATION, authHeaderValue)
				.param("timeoutMinutes", "0"))
			.andExpect(status().isOk());

		mockMvc.perform(get("/trips/{id}", tripId).header(HttpHeaders.AUTHORIZATION, authHeaderValue))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("DRIVER_ASSIGNED"));
	}

	@Test
	void scenario3_tripCanBeCompletedAfterLongOfflineGap() throws Exception {
		Integer tripId = createTripAndGetId(700, 6.0, 2.0, null);
		mockMvc.perform(patch("/trips/{id}/status", tripId).header(HttpHeaders.AUTHORIZATION, authHeaderValue)
				.contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DRIVER_ACCEPTED\"}"))
			.andExpect(status().isOk());
		mockMvc.perform(patch("/trips/{id}/status", tripId).header(HttpHeaders.AUTHORIZATION, authHeaderValue)
				.contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"IN_PROGRESS\"}"))
			.andExpect(status().isOk());
		Thread.sleep(150);
		mockMvc.perform(patch("/trips/{id}/status", tripId).header(HttpHeaders.AUTHORIZATION, authHeaderValue)
				.contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"COMPLETED\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("COMPLETED"));
	}

	@Test
	void scenario4_cancelAfterDriverMovedReleasesDriver() throws Exception {
		Integer tripId = createTripAndGetId(701, 6.0, 2.0, null);
		mockMvc.perform(patch("/trips/{id}/status", tripId).header(HttpHeaders.AUTHORIZATION, authHeaderValue)
				.contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DRIVER_ACCEPTED\"}"))
			.andExpect(status().isOk());
		mockMvc.perform(patch("/trips/{id}/status", tripId).header(HttpHeaders.AUTHORIZATION, authHeaderValue)
				.contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CANCELLED\"}"))
			.andExpect(status().isOk());

		mockMvc.perform(get("/trips/drivers/available").header(HttpHeaders.AUTHORIZATION, authHeaderValue))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(2));
	}

	@Test
	void scenario5_noAvailableDriversReturnsConflict() throws Exception {
		driverSlotRepository.deleteAll();
		mockMvc.perform(post("/trips")
				.header(HttpHeaders.AUTHORIZATION, authHeaderValue)
				.contentType(MediaType.APPLICATION_JSON)
				.content(tripPayload(702, 5.0, 2.0)))
			.andExpect(status().isConflict());
	}

	@Test
	void scenario6_defaultRatingAppliedAfterWindow() throws Exception {
		Integer tripId = createTripAndGetId(800, 10.0, 4.0, null);
		toCompleted(tripId);
		mockMvc.perform(post("/trips/maintenance/auto-rate").header(HttpHeaders.AUTHORIZATION, authHeaderValue)
				.param("hours", "0").param("rating", "3"))
			.andExpect(status().isOk());
		mockMvc.perform(get("/trips/{id}", tripId).header(HttpHeaders.AUTHORIZATION, authHeaderValue))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.rating").value(3));
	}

	@Test
	void scenario7_concurrentOrdersOneDriverOnly() throws Exception {
		driverSlotRepository.deleteAll();
		driverSlotRepository.save(driverSlot(99L, DriverAvailabilityStatus.AVAILABLE));
		String payload = tripPayload(900, 6.0, 2.0);
		ExecutorService pool = Executors.newFixedThreadPool(2);
		try {
			Callable<Integer> task = () -> mockMvc.perform(post("/trips")
					.header(HttpHeaders.AUTHORIZATION, authHeaderValue)
					.contentType(MediaType.APPLICATION_JSON).content(payload))
				.andReturn().getResponse().getStatus();
			List<Future<Integer>> futures = new ArrayList<>();
			futures.add(pool.submit(task));
			futures.add(pool.submit(task));
			int success = 0;
			int conflict = 0;
			for (Future<Integer> f : futures) {
				int c = f.get();
				if (c == 201) success++;
				if (c == 409) conflict++;
			}
			org.junit.jupiter.api.Assertions.assertEquals(1, success);
			org.junit.jupiter.api.Assertions.assertEquals(1, conflict);
		} finally {
			pool.shutdownNow();
		}
	}

	@Test
	void scenario8_invalidJwtAndMissingJwtRejected() throws Exception {
		mockMvc.perform(get("/trips/stats"))
			.andExpect(status().isForbidden());
		mockMvc.perform(get("/trips/stats").header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
			.andExpect(status().isForbidden());
		mockMvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"manager\",\"password\":\"wrong\"}"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void scenario9_validationErrorsReturn400() throws Exception {
		String invalid = """
			{"passengerId": 1, "origin":"A", "destination":"B", "distance":0, "tariff":-1}
			""";
		mockMvc.perform(post("/trips")
				.header(HttpHeaders.AUTHORIZATION, authHeaderValue)
				.contentType(MediaType.APPLICATION_JSON)
				.content(invalid))
			.andExpect(status().isBadRequest());
	}

	@Test
	void scenario10_repeatRatingRejected() throws Exception {
		Integer tripId = createTripAndGetId(901, 7.0, 2.0, null);
		toCompleted(tripId);
		mockMvc.perform(patch("/trips/{id}/rating", tripId).header(HttpHeaders.AUTHORIZATION, authHeaderValue)
				.contentType(MediaType.APPLICATION_JSON).content("{\"rating\":5}"))
			.andExpect(status().isOk());
		mockMvc.perform(patch("/trips/{id}/rating", tripId).header(HttpHeaders.AUTHORIZATION, authHeaderValue)
				.contentType(MediaType.APPLICATION_JSON).content("{\"rating\":4}"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void scenario11_invalidStatusTransitionRejected() throws Exception {
		Integer tripId = createTripAndGetId(902, 5.0, 2.0, null);
		mockMvc.perform(patch("/trips/{id}/status", tripId).header(HttpHeaders.AUTHORIZATION, authHeaderValue)
				.contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"COMPLETED\"}"))
			.andExpect(status().isBadRequest());
	}

	private DriverSlot driverSlot(Long driverId, DriverAvailabilityStatus status) {
		DriverSlot slot = new DriverSlot();
		slot.setDriverId(driverId);
		slot.setStatus(status);
		return slot;
	}

	private Integer createTripAndGetId(int passengerId, double distance, double tariff, String idemKey) throws Exception {
		var req = post("/trips")
			.header(HttpHeaders.AUTHORIZATION, authHeaderValue)
			.contentType(MediaType.APPLICATION_JSON)
			.content(tripPayload(passengerId, distance, tariff));
		if (idemKey != null) {
			req.header("Idempotency-Key", idemKey);
		}
		MvcResult res = mockMvc.perform(req).andExpect(status().isCreated()).andReturn();
		Map<?, ?> body = objectMapper.readValue(res.getResponse().getContentAsString(), Map.class);
		return (Integer) body.get("id");
	}

	private void toCompleted(Integer tripId) throws Exception {
		mockMvc.perform(patch("/trips/{id}/status", tripId).header(HttpHeaders.AUTHORIZATION, authHeaderValue)
				.contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DRIVER_ACCEPTED\"}"))
			.andExpect(status().isOk());
		mockMvc.perform(patch("/trips/{id}/status", tripId).header(HttpHeaders.AUTHORIZATION, authHeaderValue)
				.contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"IN_PROGRESS\"}"))
			.andExpect(status().isOk());
		mockMvc.perform(patch("/trips/{id}/status", tripId).header(HttpHeaders.AUTHORIZATION, authHeaderValue)
				.contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"COMPLETED\"}"))
			.andExpect(status().isOk());
	}

	private String tripPayload(int passengerId, double distance, double tariff) {
		return String.format(Locale.US, """
			{
			  "passengerId": %d,
			  "origin": "Point A",
			  "destination": "Point B",
			  "distance": %.1f,
			  "tariff": %.1f
			}
			""", passengerId, distance, tariff);
	}

	private String issueToken() throws Exception {
		String loginPayload = """
			{
			  "username": "manager",
			  "password": "manager123"
			}
			""";
		MvcResult result = mockMvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginPayload))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.token").isString())
			.andReturn();
		Map<?, ?> body = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
		return (String) body.get("token");
	}
}
