import java.util.List;
import java.util.Objects;
import java.util.Scanner;

public final class ConsoleUtils {
    private ConsoleUtils() {}

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BOLD = "\u001B[1m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_CYAN = "\u001B[36m";

    private static final boolean ANSI_ENABLED = detectAnsiSupport();

    public static String promptString(Scanner scanner, String message, boolean required) {
        Objects.requireNonNull(scanner, "scanner не может быть null");
        Objects.requireNonNull(message, "message не может быть null");

        while (true) {
            printPrompt(message, required ? " (обязательно)" : " (Enter = пропустить)");
            String s = scanner.nextLine();
            s = s == null ? "" : s.trim();

            if (!required) {
                return s;
            }

            if (!s.isEmpty()) {
                return s;
            }
            printError("Значение не может быть пустым. Повторите ввод.");
        }
    }

    public static int promptInt(Scanner scanner, String message, int min, int max) {
        Objects.requireNonNull(scanner, "scanner не может быть null");
        Objects.requireNonNull(message, "message не может быть null");
        if (min > max) {
            throw new IllegalArgumentException("min не может быть больше max");
        }

        while (true) {
            printPrompt(message, " (" + min + "…" + max + ")");
            String s = scanner.nextLine();
            s = s == null ? "" : s.trim();

            try {
                int v = Integer.parseInt(s);
                if (v < min || v > max) {
                    printWarn("Введите число в диапазоне " + min + "…" + max + ".");
                    continue;
                }
                return v;
            } catch (NumberFormatException e) {
                printWarn("Введите корректное целое число.");
            }
        }
    }

    public static boolean promptYesNo(Scanner scanner, String message) {
        Objects.requireNonNull(scanner, "scanner не может быть null");
        Objects.requireNonNull(message, "message не может быть null");

        while (true) {
            printPrompt(message, " (да/нет)");
            String s = scanner.nextLine();
            s = s == null ? "" : s.trim().toLowerCase();

            if (s.isEmpty()) {
                printWarn("Введите 'да' или 'нет'.");
                continue;
            }

            if (isYes(s)) return true;
            if (isNo(s)) return false;

            printWarn("Не понял ответ. Введите 'да' или 'нет'.");
        }
    }

    public static <T> T promptChoice(Scanner scanner, String message, List<T> options) {
        Objects.requireNonNull(scanner, "scanner не может быть null");
        Objects.requireNonNull(message, "message не может быть null");
        Objects.requireNonNull(options, "options не может быть null");
        if (options.isEmpty()) {
            throw new IllegalArgumentException("options не может быть пустым");
        }

        System.out.println(formatHeader(message));
        for (int i = 0; i < options.size(); i++) {
            T opt = options.get(i);
            System.out.println("  " + (i + 1) + ") " + String.valueOf(opt));
        }
        int idx = promptInt(scanner, "Выберите номер", 1, options.size());
        return options.get(idx - 1);
    }

    public static String formatHeader(String title) {
        String t = title == null ? "" : title.trim();
        if (t.isEmpty()) return "";

        String line = "─".repeat(Math.max(8, Math.min(80, t.length() + 6)));
        String header = "┌" + line + "┐" + System.lineSeparator()
                + "│  " + t + "  │" + System.lineSeparator()
                + "└" + line + "┘";
        return ANSI_ENABLED ? (ANSI_CYAN + ANSI_BOLD + header + ANSI_RESET) : header;
    }

    private static void printPrompt(String message, String hint) {
        String m = message.trim();
        String suffix = hint == null ? "" : hint;
        String prefix = ANSI_ENABLED ? (ANSI_CYAN + ANSI_BOLD + "> " + ANSI_RESET) : "> ";
        System.out.print(prefix + m + suffix + ": ");
    }

    private static void printWarn(String msg) {
        if (ANSI_ENABLED) {
            System.out.println(ANSI_YELLOW + msg + ANSI_RESET);
        } else {
            System.out.println(msg);
        }
    }

    private static void printError(String msg) {
        if (ANSI_ENABLED) {
            System.out.println(ANSI_RED + msg + ANSI_RESET);
        } else {
            System.out.println(msg);
        }
    }

    private static boolean isYes(String s) {
        return s.equals("да") || s.equals("д") || s.equals("y") || s.equals("yes") || s.equals("true") || s.equals("1");
    }

    private static boolean isNo(String s) {
        return s.equals("нет") || s.equals("н") || s.equals("n") || s.equals("no") || s.equals("false") || s.equals("0");
    }

    private static boolean detectAnsiSupport() {
        String force = System.getProperty("console.ansi");
        if (force != null) {
            return force.equalsIgnoreCase("true") || force.equals("1") || force.equalsIgnoreCase("on");
        }

        String os = System.getProperty("os.name", "").toLowerCase();
        boolean isWindows = os.contains("win");
        if (!isWindows) {
            return System.console() != null;
        }

        return System.getenv("WT_SESSION") != null
                || System.getenv("TERM") != null
                || System.getenv("ANSICON") != null
                || System.getenv("ConEmuANSI") != null;
    }
}

