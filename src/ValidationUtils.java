public class ValidationUtils {
    private static final String USERNAME_REGEX = "^[a-zA-Z0-9_]+$";
    private static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    private static final String DATE_REGEX = "^\\d{4}-\\d{2}-\\d{2}(?: \\d{2}:\\d{2}(?::\\d{2})?)?$";

    public static boolean isValidUsername(String username) {
        if (username == null) {
            return false;
        }
        String normalized = username.trim();
        if (normalized.length() < 3 || normalized.length() > 20) {
            return false;
        }
        return normalized.matches(USERNAME_REGEX);
    }

    public static boolean isValidEmail(String email) {
        if (email == null) {
            return false;
        }
        String normalized = email.trim();
        if (normalized.isEmpty() || normalized.length() > 254) {
            return false;
        }
        return normalized.matches(EMAIL_REGEX);
    }

    public static boolean isValidDate(String date) {
        if (date == null) {
            return false;
        }
        String normalized = date.trim();
        if (normalized.isEmpty() || !normalized.matches(DATE_REGEX)) {
            return false;
        }

        int year = parseIntSafe(normalized, 0, 4);
        int month = parseIntSafe(normalized, 5, 7);
        int day = parseIntSafe(normalized, 8, 10);
        if (year < 1 || month < 1 || month > 12 || day < 1 || day > 31) {
            return false;
        }

        if (normalized.length() == 10) {
            return true;
        }

        int hour = parseIntSafe(normalized, 11, 13);
        int minute = parseIntSafe(normalized, 14, 16);
        if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
            return false;
        }

        if (normalized.length() == 16) {
            return true;
        }

        int second = parseIntSafe(normalized, 17, 19);
        return second >= 0 && second <= 59;
    }

    public static String normalizeString(String input) {
        if (input == null) {
            return null;
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        return trimmed.replaceAll("\\s+", " ");
    }

    public static void requireNonEmpty(String value, String fieldName){
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " не может быть пустым");
        }
    }

    private static int parseIntSafe(String s, int startInclusive, int endExclusive) {
        try {
            return Integer.parseInt(s.substring(startInclusive, endExclusive));
        } catch (Exception e) {
            return -1;
        }
    }
}
