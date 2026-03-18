import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FormatUtilsTest {

    @Test
    void formatBoxWrapsTextWithBorder() {
        String box = FormatUtils.formatBox("Hello");
        String normalized = normalizeNl(box);

        assertTrue(normalized.startsWith("+"));
        assertTrue(normalized.contains("| Hello |"));
        assertTrue(normalized.endsWith("+"));
    }

    @Test
    void truncateWorksWithSmallMaxLength() {
        assertEquals("", FormatUtils.truncate("", 0));
        assertEquals(".", FormatUtils.truncate("abcdef", 1));
        assertEquals("..", FormatUtils.truncate("abcdef", 2));
        assertEquals("...", FormatUtils.truncate("abcdef", 3));
        assertEquals("a...", FormatUtils.truncate("abcdef", 4));
        assertEquals("abcdef", FormatUtils.truncate("abcdef", 6));
        assertThrows(IllegalArgumentException.class, () -> FormatUtils.truncate("x", -1));
    }

    @Test
    void padLeftAndRightPadToLength() {
        assertEquals("", FormatUtils.padLeft("x", 0));
        assertEquals("", FormatUtils.padRight("x", 0));

        assertEquals("x", FormatUtils.padLeft("x", 1));
        assertEquals("x", FormatUtils.padRight("x", 1));

        assertEquals("  x", FormatUtils.padLeft("x", 3));
        assertEquals("x  ", FormatUtils.padRight("x", 3));
    }

    @Test
    void formatTableBuildsAsciiTableWithHeadersAndRows() {
        String table = FormatUtils.formatTable(
                new String[]{"Username", "Email"},
                List.of(
                        new String[]{"admin", "admin@company.com"},
                        new String[]{"john", "john@company.com"}
                )
        );

        String normalized = normalizeNl(table);
        assertTrue(normalized.contains("| Username |"));
        assertTrue(normalized.contains("| admin"));
        assertTrue(normalized.contains("admin@company.com"));
        assertTrue(normalized.contains("| john"));
        assertTrue(normalized.startsWith("+"));
        assertTrue(normalized.endsWith("+"));
    }

    private static String normalizeNl(String s) {
        return s.replace("\r\n", "\n").replace('\r', '\n');
    }
}

