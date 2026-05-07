package com.example.taxi.notification;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.taxi.notification.domain.NotificationTask;
import com.example.taxi.notification.domain.NotificationTaskStatus;
import com.example.taxi.notification.repo.NotificationTaskRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationServiceApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private NotificationTaskRepository repository;

	@Test
	void createAndGetByTripWorks() throws Exception {
		String payload = """
			{
			  "tripId": 77,
			  "recipientType": "PASSENGER",
			  "recipientId": 501,
			  "message": "Trip accepted"
			}
			""";

		mockMvc.perform(post("/notifications")
				.contentType(MediaType.APPLICATION_JSON)
				.content(payload))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("PENDING"));

		mockMvc.perform(get("/notifications").param("trip_id", "77"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].recipientType").value("PASSENGER"));
	}

	@Test
	void workerProcessesPendingTaskToSent() throws Exception {
		String payload = """
			{
			  "tripId": 88,
			  "recipientType": "DRIVER",
			  "recipientId": 601,
			  "message": "Trip status changed"
			}
			""";

		MvcResult result = mockMvc.perform(post("/notifications")
				.contentType(MediaType.APPLICATION_JSON)
				.content(payload))
			.andExpect(status().isCreated())
			.andReturn();

		Map<?, ?> body = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
		Long taskId = ((Number) body.get("id")).longValue();

		NotificationTaskStatus status = awaitStatus(taskId, 2500);
		Assertions.assertEquals(NotificationTaskStatus.SENT, status);
	}

	@Test
	void workerRetriesAndMarksFailedAfterThreeAttempts() throws Exception {
		String payload = """
			{
			  "tripId": 99,
			  "recipientType": "PASSENGER",
			  "recipientId": 777,
			  "message": "[FAIL] force retries"
			}
			""";

		MvcResult result = mockMvc.perform(post("/notifications")
				.contentType(MediaType.APPLICATION_JSON)
				.content(payload))
			.andExpect(status().isCreated())
			.andReturn();

		Map<?, ?> body = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
		Long taskId = ((Number) body.get("id")).longValue();

		NotificationTaskStatus status = awaitStatus(taskId, 4000);
		NotificationTask task = repository.findById(taskId).orElseThrow();

		Assertions.assertEquals(NotificationTaskStatus.FAILED, status);
		Assertions.assertEquals(3, task.getAttempts());
	}

	private NotificationTaskStatus awaitStatus(Long taskId, long timeoutMs) throws InterruptedException {
		long deadline = System.currentTimeMillis() + timeoutMs;
		NotificationTaskStatus status = null;
		while (System.currentTimeMillis() < deadline) {
			NotificationTask task = repository.findById(taskId).orElse(null);
			if (task != null) {
				status = task.getStatus();
				if (status == NotificationTaskStatus.SENT || status == NotificationTaskStatus.FAILED) {
					return status;
				}
			}
			Thread.sleep(80);
		}
		return status;
	}
}
