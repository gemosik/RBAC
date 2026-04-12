import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AuditLogTest {

    @Test
    void logAddsEntriesAndFiltersWork() throws Exception {
        try (AuditLog log = new AuditLog()) {
            log.log("USER_CREATE", "admin", "john", "ok");
            log.log("ROLE_CREATE", "admin", "Manager", "permissions=2");
            log.log("USER_CREATE", "system", "alice", "");

            log.awaitIdle(5000);

            List<AuditEntry> all = log.getAll();
            assertEquals(3, all.size());

            assertEquals(2, log.getByAction("USER_CREATE").size());
            assertEquals(2, log.getByPerformer("admin").size());
            assertEquals(1, log.getByPerformer("system").size());
        }
    }

    @Test
    void printLogHandlesEmpty() throws Exception {
        try (AuditLog log = new AuditLog()) {
            log.awaitIdle(1000);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PrintStream original = System.out;
            try {
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
                log.printLog();
            } finally {
                System.setOut(original);
            }

            String printed = out.toString(StandardCharsets.UTF_8);
            assertTrue(printed.contains("audit log пуст"));
        }
    }

    @Test
    void saveToFileWritesFormattedLog() throws Exception {
        try (AuditLog log = new AuditLog()) {
            log.log("USER_CREATE", "admin", "john", "email=john@example.com");
            log.awaitIdle(5000);

            Path tmp = Files.createTempFile("rbac-audit-", ".log");
            tmp.toFile().deleteOnExit();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PrintStream original = System.out;
            try {
                System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
                log.saveToFile(tmp.toString());
            } finally {
                System.setOut(original);
            }

            String file = Files.readString(tmp, StandardCharsets.UTF_8);
            assertTrue(file.contains("Audit Log"));
            assertTrue(file.contains("USER_CREATE"));
            assertTrue(file.contains("admin"));
            assertTrue(file.contains("john"));
            assertTrue(file.contains("email=john@example.com"));
        }
    }
}
