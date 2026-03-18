import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
 
public final class FormatUtils {
    private FormatUtils() {}
 
    public static String formatTable(String[] headers, List<String[]> rows) {
        Objects.requireNonNull(headers, "headers не может быть null");
        Objects.requireNonNull(rows, "rows не может быть null");
 
        int cols = headers.length;
        for (String[] r : rows) {
            if (r != null) {
                cols = Math.max(cols, r.length);
            }
        }
        if (cols == 0) {
            return "";
        }
 
        String[] normalizedHeaders = new String[cols];
        for (int i = 0; i < cols; i++) {
            normalizedHeaders[i] = i < headers.length ? safe(headers[i]) : "";
        }
 
        List<String[]> normalizedRows = new ArrayList<>(rows.size());
        for (String[] r : rows) {
            String[] nr = new String[cols];
            if (r != null) {
                for (int i = 0; i < Math.min(cols, r.length); i++) {
                    nr[i] = safe(r[i]);
                }
            }
            for (int i = 0; i < cols; i++) {
                if (nr[i] == null) nr[i] = "";
            }
            normalizedRows.add(nr);
        }
 
        int[] widths = new int[cols];
        for (int c = 0; c < cols; c++) {
            widths[c] = normalizedHeaders[c].length();
        }
        for (String[] r : normalizedRows) {
            for (int c = 0; c < cols; c++) {
                widths[c] = Math.max(widths[c], safe(r[c]).length());
            }
        }
 
        String border = buildBorder(widths);
        StringBuilder out = new StringBuilder();
        out.append(border).append(System.lineSeparator());
        out.append(buildRow(normalizedHeaders, widths)).append(System.lineSeparator());
        out.append(border).append(System.lineSeparator());
        for (String[] r : normalizedRows) {
            out.append(buildRow(r, widths)).append(System.lineSeparator());
        }
        out.append(border);
        return out.toString();
    }
 
    public static String formatBox(String text) {
        String t = safe(text);
        List<String> lines = splitLines(t);
        int width = 0;
        for (String line : lines) {
            width = Math.max(width, safe(line).length());
        }
 
        String top = "+" + "-".repeat(width + 2) + "+";
        String bottom = top;
 
        StringBuilder sb = new StringBuilder();
        sb.append(top).append(System.lineSeparator());
        for (String line : lines) {
            sb.append("| ")
                    .append(padRight(safe(line), width))
                    .append(" |")
                    .append(System.lineSeparator());
        }
        sb.append(bottom);
        return sb.toString();
    }
 
    public static String formatHeader(String text) {
        String t = safe(text).trim();
        if (t.isEmpty()) return "";
        String line = repeat('=', Math.max(8, t.length()));
        return line + System.lineSeparator()
                + t + System.lineSeparator()
                + line;
    }
 
    public static String truncate(String text, int maxLength) {
        String t = safe(text);
        if (maxLength < 0) {
            throw new IllegalArgumentException("maxLength не может быть отрицательным");
        }
        if (t.length() <= maxLength) return t;
        if (maxLength <= 3) return ".".repeat(maxLength);
        return t.substring(0, maxLength - 3) + "...";
    }
 
    public static String padRight(String text, int length) {
        String t = safe(text);
        if (length <= 0) return "";
        if (t.length() >= length) return t;
        return t + " ".repeat(length - t.length());
    }
 
    public static String padLeft(String text, int length) {
        String t = safe(text);
        if (length <= 0) return "";
        if (t.length() >= length) return t;
        return " ".repeat(length - t.length()) + t;
    }
 
    private static String buildBorder(int[] widths) {
        StringBuilder sb = new StringBuilder();
        sb.append("+");
        for (int c = 0; c < widths.length; c++) {
            sb.append("-".repeat(widths[c] + 2)).append("+");
        }
        return sb.toString();
    }
 
    private static String buildRow(String[] cells, int[] widths) {
        StringBuilder sb = new StringBuilder();
        sb.append("|");
        for (int c = 0; c < widths.length; c++) {
            String cell = c < cells.length ? safe(cells[c]) : "";
            sb.append(" ").append(padRight(cell, widths[c])).append(" |");
        }
        return sb.toString();
    }
 
    private static String repeat(char ch, int count) {
        if (count <= 0) return "";
        return String.valueOf(ch).repeat(count);
    }
 
    private static String safe(String s) {
        return s == null ? "" : s;
    }
 
    private static List<String> splitLines(String s) {
        String normalized = safe(s).replace("\r\n", "\n").replace('\r', '\n');
        String[] parts = normalized.split("\n", -1);
        return List.of(parts);
    }
}
