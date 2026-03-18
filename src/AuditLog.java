public class AuditLog {
    private final java.util.List<AuditEntry> entries;

    public AuditLog() {
        this.entries = new java.util.ArrayList<>();
    }

    public void log(String action, String performer, String target, String details) {
        String ts = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        entries.add(new AuditEntry(
                ts,
                nullToEmpty(action),
                nullToEmpty(performer),
                nullToEmpty(target),
                nullToEmpty(details)
        ));
    }

    public java.util.List<AuditEntry> getAll() {
        return java.util.List.copyOf(entries);
    }

    public java.util.List<AuditEntry> getByPerformer(String performer) {
        String p = nullToEmpty(performer);
        return entries.stream()
                .filter(e -> e.performer().equals(p))
                .toList();
    }

    public java.util.List<AuditEntry> getByAction(String action) {
        String a = nullToEmpty(action);
        return entries.stream()
                .filter(e -> e.action().equals(a))
                .toList();
    }

    public void printLog() {
        if (entries.isEmpty()) {
            System.out.println("(audit log пуст)");
            return;
        }
        System.out.println(formatAll());
    }

    public void saveToFile(String filename) {
        try {
            java.nio.file.Path path = java.nio.file.Path.of(filename);
            java.nio.file.Files.writeString(
                    path,
                    formatAll() + System.lineSeparator(),
                    java.nio.charset.StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
            );
            System.out.println("Audit log сохранён в файл: " + path.toAbsolutePath());
        } catch (Exception e) {
            System.out.println("Не удалось сохранить audit log: " + e.getMessage());
        }
    }

    private String formatAll() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Audit Log (").append(entries.size()).append(") ===").append(System.lineSeparator());
        for (int i = 0; i < entries.size(); i++) {
            AuditEntry e = entries.get(i);
            sb.append(i + 1).append(") ")
                    .append("[").append(e.timestamp()).append("] ")
                    .append(e.action()).append(" | ")
                    .append(e.performer()).append(" -> ").append(e.target());
            if (!e.details().isBlank()) {
                sb.append(" | ").append(e.details());
            }
            sb.append(System.lineSeparator());
        }
        sb.append("================================").append(System.lineSeparator());
        return sb.toString();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
