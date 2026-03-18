import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DateUtilsTest {

    @Test
    void getCurrentDateAndDateTimeHaveExpectedFormat() {
        DateUtils du = new DateUtils();
        String d = du.getCurrentDate();
        String dt = du.getCurrentDateTime();

        assertTrue(d.matches("\\d{4}-\\d{2}-\\d{2}"));
        assertTrue(dt.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
    }

    @Test
    void isBeforeAndAfterUseComparableStringDates() {
        DateUtils du = new DateUtils();
        assertTrue(du.isBefore("2026-01-01", "2026-01-02"));
        assertTrue(du.isAfter("2026-01-02", "2026-01-01"));
        assertFalse(du.isBefore("2026-01-01", "2026-01-01"));
        assertFalse(du.isAfter("2026-01-01", "2026-01-01"));
    }

    @Test
    void addDaysWorksInSimplifiedModel() {
        DateUtils du = new DateUtils();
        // выбираем дату с day<=30, чтобы не попадать в край упрощённой модели
        assertEquals("2026-01-02", du.addDays("2026-01-01", 1));
        assertEquals("2026-02-01", du.addDays("2026-01-30", 1));
        assertEquals("2026-01-29", du.addDays("2026-01-30", -1));
    }

    @Test
    void formatRelativeTimeReturnsTodayAndInOrAgo() {
        DateUtils du = new DateUtils();
        String today = du.getCurrentDate();
        // чтобы тест не зависел от 31-го числа (упрощённая модель addDays использует месяцы по 30 дней)
        String safeToday = today.endsWith("-31") ? today.substring(0, 8) + "30" : today;
        assertEquals("today", du.formatRelativeTime(safeToday));

        String tomorrow = du.addDays(safeToday, 1);
        assertTrue(du.formatRelativeTime(tomorrow).startsWith("in "));

        String yesterday = du.addDays(safeToday, -1);
        assertTrue(du.formatRelativeTime(yesterday).endsWith(" ago"));
    }
}

