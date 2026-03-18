import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
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

