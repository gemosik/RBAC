package com.example.taxi.user;

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
class UserServiceApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void passengerFlowWorks() throws Exception {
		String payload = """
			{
			  "name": "Alice",
			  "email": "alice@test.com",
			  "phone": "+70000000001"
			}
			""";

		MvcResult createResult = mockMvc.perform(post("/passengers")
				.contentType(MediaType.APPLICATION_JSON)
				.content(payload))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").isNumber())
			.andExpect(jsonPath("$.name").value("Alice"))
			.andReturn();

		Map<?, ?> body = objectMapper.readValue(createResult.getResponse().getContentAsString(), Map.class);
		Integer id = (Integer) body.get("id");

		mockMvc.perform(get("/passengers/{id}", id))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value("alice@test.com"));
	}

	@Test
	void driverStatusUpdateWorks() throws Exception {
		String createPayload = """
			{
			  "name": "Bob",
			  "email": "bob@test.com",
			  "phone": "+70000000002",
			  "licenseNumber": "LIC-111"
			}
			""";

		MvcResult createResult = mockMvc.perform(post("/drivers")
				.contentType(MediaType.APPLICATION_JSON)
				.content(createPayload))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("AVAILABLE"))
			.andReturn();

		Map<?, ?> body = objectMapper.readValue(createResult.getResponse().getContentAsString(), Map.class);
		Integer id = (Integer) body.get("id");

		String patchPayload = """
			{
			  "status": "BUSY"
			}
			""";

		mockMvc.perform(patch("/drivers/{id}/status", id)
				.contentType(MediaType.APPLICATION_JSON)
				.content(patchPayload))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("BUSY"));
	}

	@Test
	void getUnknownPassengerReturns404() throws Exception {
		mockMvc.perform(get("/passengers/99999"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("Passenger not found: 99999"));
	}
}
