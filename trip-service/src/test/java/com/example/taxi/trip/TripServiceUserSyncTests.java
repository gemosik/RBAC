package com.example.taxi.trip;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.taxi.trip.integration.UserServiceClient;
import com.example.taxi.trip.domain.DriverAvailabilityStatus;
import com.example.taxi.trip.domain.DriverSlot;
import com.example.taxi.trip.repo.DriverSlotRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class TripServiceUserSyncTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private DriverSlotRepository driverSlotRepository;

	@MockBean
	private UserServiceClient userServiceClient;

	@Test
	void createTripFailsWhenPassengerMissingInUserService() throws Exception {
		driverSlotRepository.deleteAll();
		DriverSlot slot = new DriverSlot();
		slot.setDriverId(333L);
		slot.setStatus(DriverAvailabilityStatus.AVAILABLE);
		driverSlotRepository.save(slot);
		Mockito.when(userServiceClient.passengerExists(999L)).thenReturn(false);
		String token = issueToken();
		String payload = """
			{"passengerId":999,"origin":"A","destination":"B","distance":1.0,"tariff":2.0}
			""";
		mockMvc.perform(post("/trips")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(payload))
			.andExpect(status().isBadRequest());
	}

	private String issueToken() throws Exception {
		String loginPayload = "{\"username\":\"manager\",\"password\":\"manager123\"}";
		String body = mockMvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginPayload))
			.andReturn().getResponse().getContentAsString();
		return new com.fasterxml.jackson.databind.ObjectMapper().readTree(body).get("token").asText();
	}
}
