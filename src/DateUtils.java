import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtils {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public String getCurrentDate() {
        return LocalDate.now().format(DATE_FORMATTER);
    }

    public String getCurrentDateTime() {
        return LocalDateTime.now().format(DATE_TIME_FORMATTER);
    }

    public boolean isBefore(String date1, String date2) {
        ValidationUtils.requireNonEmpty(date1, "date1");
        ValidationUtils.requireNonEmpty(date2, "date2");
        return normalizeComparable(date1).compareTo(normalizeComparable(date2)) < 0;
    }

    public boolean isAfter(String date1, String date2) {
        ValidationUtils.requireNonEmpty(date1, "date1");
        ValidationUtils.requireNonEmpty(date2, "date2");
        return normalizeComparable(date1).compareTo(normalizeComparable(date2)) > 0;
    }

    public String addDays(String date, int days) {
        ValidationUtils.requireNonEmpty(date, "date");
        String d = normalizeDateOnly(date);
        if (!ValidationUtils.isValidDate(d)) {
            throw new IllegalArgumentException("Неверный формат даты. Используйте YYYY-MM-DD");
        }

        int year = Integer.parseInt(d.substring(0, 4));
        int month = Integer.parseInt(d.substring(5, 7));
        int day = Integer.parseInt(d.substring(8, 10));

        int ordinal = toOrdinal360(year, month, day);
        int newOrdinal = ordinal + days;
        if (newOrdinal < 0) {
            throw new IllegalArgumentException("Результирующая дата раньше 0000-01-01 (упрощённая модель)");
        }

        int[] ymd = fromOrdinal360(newOrdinal);
        return String.format("%04d-%02d-%02d", ymd[0], ymd[1], ymd[2]);
    }

    public String formatRelativeTime(String date) {
        ValidationUtils.requireNonEmpty(date, "date");
        String d = normalizeDateOnly(date);
        if (!ValidationUtils.isValidDate(d)) {
            throw new IllegalArgumentException("Неверный формат даты. Используйте YYYY-MM-DD");
        }

        String todayStr = getCurrentDate();

        int target = toOrdinal360(
                Integer.parseInt(d.substring(0, 4)),
                Integer.parseInt(d.substring(5, 7)),
                Integer.parseInt(d.substring(8, 10))
        );
        int today = toOrdinal360(
                Integer.parseInt(todayStr.substring(0, 4)),
                Integer.parseInt(todayStr.substring(5, 7)),
                Integer.parseInt(todayStr.substring(8, 10))
        );

        int diff = target - today;
        if (diff == 0) {
            return "today";
        }

        int abs = Math.abs(diff);
        String unit = abs == 1 ? "day" : "days";
        if (diff < 0) {
            return abs + " " + unit + " ago";
        }
        return "in " + abs + " " + unit;
    }

    private static String normalizeComparable(String date) {
        String normalized = ValidationUtils.normalizeString(date);
        if (normalized == null || normalized.isEmpty()) {
            return "";
        }
        return normalized;
    }

    private static String normalizeDateOnly(String date) {
        String normalized = ValidationUtils.normalizeString(date);
        if (normalized == null || normalized.isEmpty()) {
            return "";
        }
        if (normalized.length() >= 10) {
            return normalized.substring(0, 10);
        }
        return normalized;
    }

    // Упрощённая модель: 12 месяцев по 30 дней (год = 360 дней)
    private static int toOrdinal360(int year, int month, int day) {
        if (year < 0 || month < 1 || month > 12 || day < 1 || day > 31) {
            throw new IllegalArgumentException("Некорректная дата для упрощённой модели");
        }
        return year * 360 + (month - 1) * 30 + (day - 1);
    }

    private static int[] fromOrdinal360(int ordinal) {
        int year = ordinal / 360;
        int rem = ordinal % 360;
        int month = (rem / 30) + 1;
        int day = (rem % 30) + 1;
        return new int[]{year, month, day};
    }
}
