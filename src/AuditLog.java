import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public class AuditLog implements AutoCloseable {

    private static final DateTimeFormatter TS = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final List<AuditEntry> entries = new ArrayList<>();
    private final BlockingQueue<AuditEntry> queue = new LinkedBlockingQueue<>();
    private final AtomicLong offered = new AtomicLong();
    private final AtomicLong processed = new AtomicLong();

    private final Thread worker;
    private volatile boolean closed;

    public AuditLog() {
        worker = new Thread(this::processLoop, "audit-log-worker");
        worker.setDaemon(true);
        worker.start();
    }

    public void log(String action, String performer, String target, String details) {
        String ts = LocalDateTime.now().format(TS);
        AuditEntry entry = new AuditEntry(
                ts,
                nullToEmpty(action),
                nullToEmpty(performer),
                nullToEmpty(target),
                nullToEmpty(details)
        );
        if (closed) {
            synchronized (entries) {
                entries.add(entry);
            }
            offered.incrementAndGet();
            processed.incrementAndGet();
            return;
        }
        offered.incrementAndGet();
        try {
            queue.put(entry);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            synchronized (entries) {
                entries.add(entry);
            }
            processed.incrementAndGet();
        }
    }

    private void processLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                AuditEntry e = queue.take();
                synchronized (entries) {
                    entries.add(e);
                }
                processed.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        drainQueueToEntries();
    }

    private void drainQueueToEntries() {
        AuditEntry e;
        while ((e = queue.poll()) != null) {
            synchronized (entries) {
                entries.add(e);
            }
            processed.incrementAndGet();
        }
    }

    public void awaitIdle(long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (processed.get() != offered.get()) {
            if (System.currentTimeMillis() > deadline) {
                throw new InterruptedException(
                        "audit log idle timeout: offered=" + offered.get() + " processed=" + processed.get()
                );
            }
            Thread.sleep(1);
        }
    }

    public List<AuditEntry> getAll() {
        synchronized (entries) {
            return List.copyOf(entries);
        }
    }

    public List<AuditEntry> getByPerformer(String performer) {
        String p = nullToEmpty(performer);
        synchronized (entries) {
            return entries.stream()
                    .filter(e -> e.performer().equals(p))
                    .toList();
        }
    }

    public List<AuditEntry> getByAction(String action) {
        String a = nullToEmpty(action);
        synchronized (entries) {
            return entries.stream()
                    .filter(e -> e.action().equals(a))
                    .toList();
        }
    }

    public void printLog() {
        try {
            awaitIdle(60_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        synchronized (entries) {
            if (entries.isEmpty()) {
                System.out.println("(audit log пуст)");
                return;
            }
        }
        System.out.println(formatAll());
    }

    public void saveToFile(String filename) {
        try {
            awaitIdle(60_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        try {
            Path path = Path.of(filename);
            Files.writeString(
                    path,
                    formatAll() + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
            System.out.println("Audit log сохранён в файл: " + path.toAbsolutePath());
        } catch (Exception e) {
            System.out.println("Не удалось сохранить audit log: " + e.getMessage());
        }
    }

    private String formatAll() {
        synchronized (entries) {
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
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    public void shutdown() {
        closed = true;
        worker.interrupt();
        try {
            worker.join(10_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        drainQueueToEntries();
    }

    @Override
    public void close() {
        shutdown();
    }
}
