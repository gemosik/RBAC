import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

public class CommandAuditIntegrationTest {

    @Test
    void userCreateAndDeleteCancelledAreLogged() {
        RBACSystem system = new RBACSystem();
        system.initialize();
        system.setCurrentUser("admin");

        CommandParser parser = new CommandParser();
        CommandRegistry.registerAll(parser);

        // user-create: username, fullName, email
        runWithCapturedOut(() ->
                parser.parseAndExecute(
                        "user-create",
                        new Scanner("john_test\nJohn Test\njohn_test@example.com\n"),
                        system
                )
        );

        system.awaitAuditIdle(5000);
        assertEquals(1, system.getAuditLog().getByAction("USER_CREATE").size());
        assertEquals("admin", system.getAuditLog().getByAction("USER_CREATE").get(0).performer());

        // user-delete: username, confirm=no
        runWithCapturedOut(() ->
                parser.parseAndExecute(
                        "user-delete",
                        new Scanner("john_test\nнет\n"),
                        system
                )
        );

        system.awaitAuditIdle(5000);
        assertEquals(1, system.getAuditLog().getByAction("USER_DELETE_CANCELLED").size());
    }

    @Test
    void roleCreateIsLogged() {
        RBACSystem system = new RBACSystem();
        system.initialize();
        system.setCurrentUser("admin");

        CommandParser parser = new CommandParser();
        CommandRegistry.registerAll(parser);

        // role-create: name, description, add-permission? (no)
        runWithCapturedOut(() ->
                parser.parseAndExecute(
                        "role-create",
                        new Scanner("NewRole\nRole Description\nнет\n"),
                        system
                )
        );

        system.awaitAuditIdle(5000);
        assertEquals(1, system.getAuditLog().getByAction("ROLE_CREATE").size());
        assertEquals("NewRole", system.getAuditLog().getByAction("ROLE_CREATE").get(0).target());
    }

    @Test
    void auditLogCommandPrintsAndCanSkipSaving() {
        RBACSystem system = new RBACSystem();
        system.initialize();
        system.setCurrentUser("admin");
        system.getAuditLog().log("PING", "admin", "system", "ok");

        CommandParser parser = new CommandParser();
        CommandRegistry.registerAll(parser);

        String printed = runWithCapturedOut(() ->
                parser.parseAndExecute(
                        "audit-log",
                        new Scanner("нет\n"),
                        system
                )
        );

        system.awaitAuditIdle(5000);
        assertTrue(printed.contains("Audit Log"));
        assertTrue(printed.contains("PING"));
    }

    @Test
    void reportCommandsPrintReportsAndCanSkipSaving() {
        RBACSystem system = new RBACSystem();
        system.initialize();

        CommandParser parser = new CommandParser();
        CommandRegistry.registerAll(parser);

        String outUsers = runWithCapturedOut(() ->
                parser.parseAndExecute("report-users", new Scanner("нет\n"), system)
        );
        assertTrue(outUsers.contains("Отчёт по пользователям"));

        String outRoles = runWithCapturedOut(() ->
                parser.parseAndExecute("report-roles", new Scanner("нет\n"), system)
        );
        assertTrue(outRoles.contains("Отчёт по ролям"));

        String outMatrix = runWithCapturedOut(() ->
                parser.parseAndExecute("report-matrix", new Scanner("нет\n"), system)
        );
        assertTrue(outMatrix.contains("Матрица прав"));
    }

    @Test
    void reportUsersAsyncEventuallyPrintsReport() throws Exception {
        RBACSystem system = new RBACSystem();
        system.initialize();

        CommandParser parser = new CommandParser();
        CommandRegistry.registerAll(parser);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream original = System.out;
        try {
            System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
            parser.parseAndExecute("report-users-async", new Scanner(""), system);
            waitForSubstring(out, "Отчёт по пользователям", 10_000);
        } finally {
            System.setOut(original);
        }

        String printed = out.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("готово"));
        assertTrue(printed.contains("admin"));
    }

    @Test
    void saveAsyncWritesAuditFile() throws Exception {
        RBACSystem system = new RBACSystem();
        system.initialize();
        system.getAuditLog().log("PING", "admin", "test", "async-save");
        system.awaitAuditIdle(5000);

        Path tmp = Files.createTempFile("rbac-save-async-", ".log");
        tmp.toFile().deleteOnExit();

        CommandParser parser = new CommandParser();
        CommandRegistry.registerAll(parser);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream original = System.out;
        try {
            System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
            parser.parseAndExecute("save-async", new Scanner(tmp + "\n"), system);
            waitForFileContains(tmp, "PING", 10_000);
        } finally {
            System.setOut(original);
        }

        assertTrue(out.toString(StandardCharsets.UTF_8).contains("Сохранение audit log"));
        system.shutdown();
    }

    private static void waitForSubstring(ByteArrayOutputStream out, String needle, long maxWaitMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + maxWaitMs;
        while (System.currentTimeMillis() < deadline) {
            if (out.toString(StandardCharsets.UTF_8).contains(needle)) {
                return;
            }
            Thread.sleep(20);
        }
        fail("timeout waiting for output containing: " + needle);
    }

    private static void waitForFileContains(Path path, String needle, long maxWaitMs) throws Exception {
        long deadline = System.currentTimeMillis() + maxWaitMs;
        while (System.currentTimeMillis() < deadline) {
            if (Files.isRegularFile(path)) {
                String s = Files.readString(path, StandardCharsets.UTF_8);
                if (s.contains(needle)) {
                    return;
                }
            }
            Thread.sleep(20);
        }
        fail("timeout waiting for file content: " + path);
    }

    private static String runWithCapturedOut(Runnable fn) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream original = System.out;
        try {
            System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
            fn.run();
            return out.toString(StandardCharsets.UTF_8);
        } finally {
            System.setOut(original);
        }
    }
}

