import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

public class CommandRegistryTest {

    @Test
    void userCreateThenViewShowsRolesAndPermissions() {
        RBACSystem system = new RBACSystem();
        system.initialize();
        system.setCurrentUser("admin");

        CommandParser parser = new CommandParser();
        CommandRegistry.registerAll(parser);

        Scanner in = new Scanner(String.join("\n",
                "tester",
                "Test User",
                "tester@example.com",
                "tester"
        ));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream original = System.out;
        try {
            System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
            parser.parseAndExecute("user-create", in, system);
            parser.parseAndExecute("user-view", in, system);
        } finally {
            System.setOut(original);
        }

        assertTrue(system.getUserManager().exists("tester"));
        String printed = out.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("Пользователь создан"));
        assertTrue(printed.contains("Назначенные роли"));
        assertTrue(printed.contains("Права"));
    }

    @Test
    void assignRoleThenPermissionsCheckReturnsYes() {
        RBACSystem system = new RBACSystem();
        system.initialize();
        system.setCurrentUser("admin");

        system.getUserManager().add(User.create("user1", "User One", "u1@example.com"));

        CommandParser parser = new CommandParser();
        CommandRegistry.registerAll(parser);

        Scanner in = new Scanner(String.join("\n",
                "user1",
                "1",
                "1",
                "for test",
                "user1",
                "READ",
                "users"
        ));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream original = System.out;
        try {
            System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
            parser.parseAndExecute("assign-role", in, system);
            parser.parseAndExecute("permissions-check", in, system);
        } finally {
            System.setOut(original);
        }

        String printed = out.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("Назначение создано"));
        assertTrue(printed.contains("ДА"));
    }

    @Test
    void userDeleteRemovesAssignments() {
        RBACSystem system = new RBACSystem();
        system.initialize();
        system.setCurrentUser("admin");

        system.getUserManager().add(User.create("user2", "User Two", "u2@example.com"));

        Role adminRole = system.getRoleManager().findByName("Admin").orElseThrow();
        User u2 = system.getUserManager().findByUsername("user2").orElseThrow();
        system.getAssignmentManager().add(new PermanentAssignment(
                u2, adminRole, AssignmentMetadata.now("admin", "seed")
        ));

        assertEquals(2, system.getAssignmentManager().count());

        CommandParser parser = new CommandParser();
        CommandRegistry.registerAll(parser);

        Scanner in = new Scanner(String.join("\n",
                "user2",
                "да"
        ));

        parser.parseAndExecute("user-delete", in, system);

        assertFalse(system.getUserManager().exists("user2"));
        assertEquals(1, system.getAssignmentManager().count());
    }
}

