import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

public class CommandParserTest {

    @Test
    void parseAndExecuteRunsRegisteredCommand() {
        CommandParser parser = new CommandParser();
        RBACSystem system = new RBACSystem();
        system.initialize();

        StringBuilder sb = new StringBuilder();
        parser.registerCommand("ping", "test", (scanner, sys) -> sb.append("ok"));

        parser.parseAndExecute("ping", new Scanner(""), system);
        assertEquals("ok", sb.toString());
    }

    @Test
    void parseAndExecuteUnknownCommandPrintsMessage() {
        CommandParser parser = new CommandParser();
        RBACSystem system = new RBACSystem();
        system.initialize();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream original = System.out;
        try {
            System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
            parser.parseAndExecute("missing", new Scanner(""), system);
        } finally {
            System.setOut(original);
        }

        String printed = out.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("Неизвестная команда"));
    }

    @Test
    void helpIsHandledEvenIfNotRegistered() {
        CommandParser parser = new CommandParser();
        RBACSystem system = new RBACSystem();
        system.initialize();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream original = System.out;
        try {
            System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
            parser.parseAndExecute("help", new Scanner(""), system);
        } finally {
            System.setOut(original);
        }

        String printed = out.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("Команды не зарегистрированы"));
    }
}

