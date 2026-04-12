import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RBACConcurrentStressTest {

    private static final int THREADS = 6;
    private static final int ITERATIONS_PER_THREAD = 25;

    @Test
    void concurrentUsersRolesFiltersAndAssignmentsStayConsistent() throws Exception {
        RBACSystem system = new RBACSystem();
        try {
            system.initialize();

            int initialUsers = system.getUserManager().count();
            int initialAssignments = system.getAssignmentManager().count();

            Set<String> expectedUsernames = ConcurrentHashMap.newKeySet();
            CopyOnWriteArrayList<Throwable> failures = new CopyOnWriteArrayList<>();

            CountDownLatch ready = new CountDownLatch(THREADS);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch finished = new CountDownLatch(THREADS);

            for (int t = 0; t < THREADS; t++) {
                final int tid = t;
                Thread thread = new Thread(() -> {
                    ready.countDown();
                    try {
                        start.await();

                        UserManager um = system.getUserManager();
                        RoleManager rm = system.getRoleManager();
                        AssignmentManager am = system.getAssignmentManager();
                        Role viewer = rm.findByName("Viewer").orElseThrow();

                        for (int i = 0; i < ITERATIONS_PER_THREAD; i++) {
                            String uname = String.format("lt%02du%04d", tid, i);
                            User u = User.create(uname, "Load T" + tid, uname + "@load.test");
                            um.add(u);
                            expectedUsernames.add(uname);

                            if (um.findByUsername(uname).isEmpty()) {
                                failures.add(new AssertionError("findByUsername пусто сразу после add: " + uname));
                            }
                            if (!um.exists(uname)) {
                                failures.add(new AssertionError("exists=false после add: " + uname));
                            }

                            List<User> filtered = um.findByFilter(UserFilters.byUsernameContains(String.format("lt%02d", tid)));
                            if (!filtered.stream().map(User::username).collect(Collectors.toSet()).contains(uname)) {
                                failures.add(new AssertionError("findByFilter не видит пользователя: " + uname));
                            }

                            List<User> filteredParallel = um.findByFilterParallel(UserFilters.byUsernameContains(String.format("lt%02d", tid)));
                            if (!filteredParallel.stream().map(User::username).collect(Collectors.toSet()).contains(uname)) {
                                failures.add(new AssertionError("findByFilterParallel не видит пользователя: " + uname));
                            }

                            um.update(uname, "Upd T" + tid + " i" + i, uname + "@updated.test");
                            User updated = um.findByUsername(uname).orElseThrow();
                            if (!updated.fullName().startsWith("Upd T" + tid)) {
                                failures.add(new AssertionError("некорректное имя после update: " + updated.fullName()));
                            }

                            am.add(new PermanentAssignment(updated, viewer, AssignmentMetadata.now("stress", "load")));
                            if (!am.userHasRole(updated, viewer)) {
                                failures.add(new AssertionError("userHasRole=false после назначения: " + uname));
                            }
                        }

                        List<User> sorted = um.findAll(UserFilters.byUsernameContains(String.format("lt%02d", tid)), UserSorters.byUsername());
                        if (sorted.size() != ITERATIONS_PER_THREAD) {
                            failures.add(new AssertionError(
                                    "findAll+filter+sorter: ожидалось " + ITERATIONS_PER_THREAD + ", получено " + sorted.size() + " tid=" + tid));
                        }

                        List<User> sortedParallel = um.findAllParallel(UserFilters.byUsernameContains(String.format("lt%02d", tid)), UserSorters.byUsername());
                        if (sortedParallel.size() != ITERATIONS_PER_THREAD) {
                            failures.add(new AssertionError(
                                    "findAllParallel: ожидалось " + ITERATIONS_PER_THREAD + ", получено " + sortedParallel.size() + " tid=" + tid));
                        }
                    } catch (Throwable e) {
                        failures.add(e);
                    } finally {
                        finished.countDown();
                    }
                }, "stress-load-" + tid);
                thread.start();
            }

            assertTrue(ready.await(30, TimeUnit.SECONDS), "потоки не готовы");
            start.countDown();
            assertTrue(finished.await(120, TimeUnit.SECONDS), "таймаут выполнения нагрузочных потоков");

            system.awaitAuditIdle(30_000);

            assertTrue(failures.isEmpty(), () -> "ошибки в воркерах: " + failures);

            UserManager um = system.getUserManager();
            AssignmentManager am = system.getAssignmentManager();

            assertEquals(initialUsers + THREADS * ITERATIONS_PER_THREAD, um.count(),
                    "число пользователей не совпало с ожидаемым (дубликаты или пропуски)");

            List<User> allUsers = um.findAll();
            Set<String> distinctNames = new HashSet<>();
            for (User u : allUsers) {
                if (!distinctNames.add(u.username())) {
                    failures.add(new AssertionError("дубликат username в findAll: " + u.username()));
                }
            }
            assertTrue(failures.isEmpty(), () -> String.valueOf(failures));
            assertEquals(allUsers.size(), distinctNames.size(), "размер списка != числу уникальных имён");

            for (String uname : expectedUsernames) {
                assertTrue(um.exists(uname), "пропущен пользователь: " + uname);
            }

            assertEquals(initialAssignments + THREADS * ITERATIONS_PER_THREAD, am.count(),
                    "число назначений не совпало (пропуски или лишние записи)");

            for (int tid = 0; tid < THREADS; tid++) {
                int n = um.findByFilter(UserFilters.byUsernameContains(String.format("lt%02d", tid))).size();
                assertEquals(ITERATIONS_PER_THREAD, n, "фильтр по префиксу потока tid=" + tid);
                int np = um.findByFilterParallel(UserFilters.byUsernameContains(String.format("lt%02d", tid))).size();
                assertEquals(ITERATIONS_PER_THREAD, np, "parallel filter tid=" + tid);
            }

            Role viewer = system.getRoleManager().findByName("Viewer").orElseThrow();
            for (String uname : expectedUsernames) {
                User u = um.findByUsername(uname).orElseThrow();
                long active = am.findByUser(u).stream().filter(RoleAssignment::isActive).count();
                assertEquals(1, active, "у пользователя должно быть одно активное назначение: " + uname);
                assertTrue(am.userHasRole(u, viewer), "ожидалась роль Viewer: " + uname);
            }
        } finally {
            system.shutdown();
        }
    }
}
