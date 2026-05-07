package com.example.taxi.trip;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class TripServiceApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

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
			.andExpect(jsonPath("$.status").value("CREATED"))
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
	void getUnknownTripReturns404() throws Exception {
		mockMvc.perform(get("/trips/99999"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("Trip not found: 99999"));
	}
}
