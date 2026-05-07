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

	@BeforeEach
	void seedDrivers() {
		driverSlotRepository.deleteAll();
		driverSlotRepository.save(driverSlot(1L, DriverAvailabilityStatus.AVAILABLE));
		driverSlotRepository.save(driverSlot(2L, DriverAvailabilityStatus.AVAILABLE));
	}

	@Test
	void createAndGetTripWorks() throws Exception {
		String payload = """
			{
			  "passengerId": 100,
			  "origin": "Airport",
			  "destination": "Downtown"
			}
			""";

		MvcResult createResult = mockMvc.perform(post("/trips")
				.contentType(MediaType.APPLICATION_JSON)
				.content(payload))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("DRIVER_ASSIGNED"))
			.andExpect(jsonPath("$.driverId").isNumber())
			.andReturn();

		Map<?, ?> body = objectMapper.readValue(createResult.getResponse().getContentAsString(), Map.class);
		Integer id = (Integer) body.get("id");

		mockMvc.perform(get("/trips/{id}", id))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.origin").value("Airport"));
	}

	@Test
	void historyAndStatusUpdateWorks() throws Exception {
		String payload1 = """
			{
			  "passengerId": 500,
			  "origin": "A",
			  "destination": "B"
			}
			""";
		String payload2 = """
			{
			  "passengerId": 500,
			  "origin": "C",
			  "destination": "D"
			}
			""";

		MvcResult created = mockMvc.perform(post("/trips")
				.contentType(MediaType.APPLICATION_JSON)
				.content(payload1))
			.andExpect(status().isCreated())
			.andReturn();
		mockMvc.perform(post("/trips")
				.contentType(MediaType.APPLICATION_JSON)
				.content(payload2))
			.andExpect(status().isCreated());

		Map<?, ?> body = objectMapper.readValue(created.getResponse().getContentAsString(), Map.class);
		Integer firstTripId = (Integer) body.get("id");

		mockMvc.perform(get("/trips").param("passenger_id", "500"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(2));

		String patchPayload = """
			{
			  "status": "IN_PROGRESS"
			}
			""";
		mockMvc.perform(patch("/trips/{id}/status", firstTripId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(patchPayload))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("IN_PROGRESS"));
	}

	@Test
	void concurrentTripCreationDoesNotAssignSameDriverTwice() throws Exception {
		driverSlotRepository.deleteAll();
		driverSlotRepository.save(driverSlot(99L, DriverAvailabilityStatus.AVAILABLE));

		String payload = """
			{
			  "passengerId": 700,
			  "origin": "Point A",
			  "destination": "Point B"
			}
			""";

		ExecutorService pool = Executors.newFixedThreadPool(2);
		try {
			Callable<Integer> createTripTask = () -> mockMvc.perform(post("/trips")
					.contentType(MediaType.APPLICATION_JSON)
					.content(payload))
				.andReturn()
				.getResponse()
				.getStatus();

			List<Future<Integer>> futures = new ArrayList<>();
			futures.add(pool.submit(createTripTask));
			futures.add(pool.submit(createTripTask));

			int success = 0;
			int conflict = 0;
			for (Future<Integer> future : futures) {
				int statusCode = future.get();
				if (statusCode == 201) {
					success++;
				}
				if (statusCode == 409) {
					conflict++;
				}
			}

			org.junit.jupiter.api.Assertions.assertEquals(1, success);
			org.junit.jupiter.api.Assertions.assertEquals(1, conflict);
		} finally {
			pool.shutdownNow();
		}
	}

	@Test
	void getUnknownTripReturns404() throws Exception {
		mockMvc.perform(get("/trips/99999"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("Trip not found: 99999"));
	}

	private DriverSlot driverSlot(Long driverId, DriverAvailabilityStatus status) {
		DriverSlot slot = new DriverSlot();
		slot.setDriverId(driverId);
		slot.setStatus(status);
		return slot;
	}
}
