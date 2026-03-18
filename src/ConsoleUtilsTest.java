import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

public class ConsoleUtilsTest {

    @Test
    void promptStringRequiredRepromptsUntilNonEmpty() {
        Scanner sc = new Scanner("\n   \nvalue\n");
        String s = withCapturedOut(() -> ConsoleUtils.promptString(sc, "field", true));
        assertEquals("value", s);
    }

    @Test
    void promptStringOptionalCanReturnEmpty() {
        Scanner sc = new Scanner("\n");
        String s = withCapturedOut(() -> ConsoleUtils.promptString(sc, "field", false));
        assertEquals("", s);
    }

    @Test
    void promptIntRepromptsOnBadInputAndOutOfRange() {
        Scanner sc = new Scanner("abc\n100\n5\n");
        int v = withCapturedOutInt(() -> ConsoleUtils.promptInt(sc, "number", 1, 10));
        assertEquals(5, v);
    }

    @Test
    void promptYesNoAcceptsRussianAndEnglish() {
        assertTrue(withCapturedOutBool(() -> ConsoleUtils.promptYesNo(new Scanner("да\n"), "confirm")));
        assertFalse(withCapturedOutBool(() -> ConsoleUtils.promptYesNo(new Scanner("нет\n"), "confirm")));
        assertTrue(withCapturedOutBool(() -> ConsoleUtils.promptYesNo(new Scanner("y\n"), "confirm")));
        assertFalse(withCapturedOutBool(() -> ConsoleUtils.promptYesNo(new Scanner("n\n"), "confirm")));
    }

    @Test
    void promptChoicePrintsMenuAndReturnsSelectedOption() {
        Scanner sc = new Scanner("2\n");
        String chosen = withCapturedOut(() -> ConsoleUtils.promptChoice(sc, "Choose", List.of("A", "B", "C")));
        assertEquals("B", chosen);
    }

    private static String withCapturedOut(SupplierWithResult<String> fn) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream original = System.out;
        try {
            System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
            return fn.get();
        } finally {
            System.setOut(original);
        }
    }

    private static int withCapturedOutInt(SupplierWithResult<Integer> fn) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream original = System.out;
        try {
            System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
            return fn.get();
        } finally {
            System.setOut(original);
        }
    }

    private static boolean withCapturedOutBool(SupplierWithResult<Boolean> fn) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream original = System.out;
        try {
            System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
            return fn.get();
        } finally {
            System.setOut(original);
        }
    }

    @FunctionalInterface
    private interface SupplierWithResult<T> {
        T get();
    }
}

