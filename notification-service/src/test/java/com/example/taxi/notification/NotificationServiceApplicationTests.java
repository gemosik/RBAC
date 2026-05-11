package com.example.taxi.notification;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.taxi.notification.domain.NotificationTask;
import com.example.taxi.notification.domain.NotificationTaskStatus;
import com.example.taxi.notification.repo.NotificationTaskRepository;
import com.example.taxi.notification.worker.NotificationProcessingService;
import com.example.taxi.notification.worker.NotificationWorkerManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;


@SpringBootTest
@AutoConfigureMockMvc
class NotificationServiceApplicationTests {

    @MockBean 
    private NotificationWorkerManager workerManager;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private NotificationTaskRepository repository;

	@Autowired
	private NotificationProcessingService processingService;

	@BeforeEach
	void cleanDb() {
		repository.deleteAll();
	}

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

		Long locked = processingService.lockNextPendingTask();
		Assertions.assertEquals(taskId, locked);
		processingService.processLockedTask(taskId, "test-worker");
		NotificationTaskStatus status = repository.findById(taskId).orElseThrow().getStatus();
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

		for (int i = 0; i < 3; i++) {
			Long locked = processingService.lockNextPendingTask();
			Assertions.assertEquals(taskId, locked);
			processingService.processLockedTask(taskId, "test-worker");
		}
		NotificationTaskStatus status = repository.findById(taskId).orElseThrow().getStatus();
		NotificationTask task = repository.findById(taskId).orElseThrow();

		Assertions.assertEquals(NotificationTaskStatus.FAILED, status);
		Assertions.assertEquals(3, task.getAttempts());
	}

	@Test
	void sameTaskIsNotLockedTwiceByConcurrentWorkers() throws Exception {
		Long taskId = createNotificationTask(321L, "single-lock-check");
		ExecutorService pool = Executors.newFixedThreadPool(2);
		try {
			CountDownLatch latch = new CountDownLatch(2);
			Set<Long> lockedIds = ConcurrentHashMap.newKeySet();
			pool.submit(() -> {
				Long id = processingService.lockNextPendingTask();
				if (id != null) {
					lockedIds.add(id);
				}
				latch.countDown();
			});
			pool.submit(() -> {
				Long id = processingService.lockNextPendingTask();
				if (id != null) {
					lockedIds.add(id);
				}
				latch.countDown();
			});
			latch.await();
			Assertions.assertEquals(1, lockedIds.size());
			Assertions.assertTrue(lockedIds.contains(taskId));
		} finally {
			pool.shutdownNow();
		}
	}

	@Test
	void lockMovesTaskToInProgressBeforeProcessing() throws Exception {
		Long taskId = createNotificationTask(654L, "in-progress-check");
		Long locked = processingService.lockNextPendingTask();
		Assertions.assertEquals(taskId, locked);
		NotificationTask task = repository.findById(taskId).orElseThrow();
		Assertions.assertEquals(NotificationTaskStatus.IN_PROGRESS, task.getStatus());
	}

	@Test
	void restartRecoveryRequeuesStuckInProgressTasks() {
		NotificationTask task = new NotificationTask();
		task.setTripId(777L);
		task.setRecipientType(com.example.taxi.notification.domain.NotificationRecipientType.PASSENGER);
		task.setRecipientId(1L);
		task.setMessage("stuck task");
		task.setAttempts(0);
		task.setStatus(NotificationTaskStatus.IN_PROGRESS);
		repository.save(task);

		int recovered = processingService.requeueInProgressTasks();
		Assertions.assertEquals(1, recovered);
		NotificationTask reloaded = repository.findById(task.getId()).orElseThrow();
		Assertions.assertEquals(NotificationTaskStatus.PENDING, reloaded.getStatus());
	}

	private Long createNotificationTask(Long tripId, String message) throws Exception {
		String payload = """
			{
			  "tripId": %d,
			  "recipientType": "PASSENGER",
			  "recipientId": 901,
			  "message": "%s"
			}
			""".formatted(tripId, message);
		MvcResult result = mockMvc.perform(post("/notifications")
				.contentType(MediaType.APPLICATION_JSON)
				.content(payload))
			.andExpect(status().isCreated())
			.andReturn();
		Map<?, ?> body = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
		return ((Number) body.get("id")).longValue();
	}
}
