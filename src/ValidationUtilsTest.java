import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ValidationUtilsTest {

    @Test
    void isValidUsernameAcceptsLettersDigitsUnderscoreAndLength() {
        assertTrue(ValidationUtils.isValidUsername("abc"));
        assertTrue(ValidationUtils.isValidUsername("john_doe"));
        assertTrue(ValidationUtils.isValidUsername("John_123"));

        assertFalse(ValidationUtils.isValidUsername(null));
        assertFalse(ValidationUtils.isValidUsername(""));
        assertFalse(ValidationUtils.isValidUsername("ab")); // too short
        assertFalse(ValidationUtils.isValidUsername("a".repeat(21))); // too long
        assertFalse(ValidationUtils.isValidUsername("john-doe"));
        assertFalse(ValidationUtils.isValidUsername("john doe"));
    }

    @Test
    void isValidEmailValidatesBasicEmailShapeAndTrimming() {
        assertTrue(ValidationUtils.isValidEmail("john@example.com"));
        assertTrue(ValidationUtils.isValidEmail(" john.doe+tag@example.co.uk "));

        assertFalse(ValidationUtils.isValidEmail(null));
        assertFalse(ValidationUtils.isValidEmail(""));
        assertFalse(ValidationUtils.isValidEmail("no-at-symbol"));
        assertFalse(ValidationUtils.isValidEmail("john@"));
        assertFalse(ValidationUtils.isValidEmail("john@example"));
    }

    @Test
    void isValidDateAcceptsDateAndOptionalTime() {
        assertTrue(ValidationUtils.isValidDate("2026-03-18"));
        assertTrue(ValidationUtils.isValidDate("2026-03-18 09:05"));
        assertTrue(ValidationUtils.isValidDate("2026-03-18 09:05:59"));

        assertFalse(ValidationUtils.isValidDate(null));
        assertFalse(ValidationUtils.isValidDate(""));
        assertFalse(ValidationUtils.isValidDate("2026/03/18"));
        assertFalse(ValidationUtils.isValidDate("2026-13-01"));
        assertFalse(ValidationUtils.isValidDate("2026-12-32"));
        assertFalse(ValidationUtils.isValidDate("2026-01-01 24:00"));
        assertFalse(ValidationUtils.isValidDate("2026-01-01 23:60"));
        assertFalse(ValidationUtils.isValidDate("2026-01-01 23:59:60"));
    }

    @Test
    void normalizeStringTrimsAndCollapsesWhitespace() {
        assertNull(ValidationUtils.normalizeString(null));
        assertEquals("", ValidationUtils.normalizeString("   "));
        assertEquals("a b c", ValidationUtils.normalizeString(" a   b \t c "));
        assertEquals("John Doe", ValidationUtils.normalizeString("John   Doe"));
    }

    @Test
    void requireNonEmptyThrowsForBlank() {
        assertThrows(IllegalArgumentException.class, () -> ValidationUtils.requireNonEmpty(null, "field"));
        assertThrows(IllegalArgumentException.class, () -> ValidationUtils.requireNonEmpty("", "field"));
        assertThrows(IllegalArgumentException.class, () -> ValidationUtils.requireNonEmpty("   ", "field"));

        assertDoesNotThrow(() -> ValidationUtils.requireNonEmpty("x", "field"));
    }
}

