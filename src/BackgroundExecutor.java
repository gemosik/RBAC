import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class BackgroundExecutor implements AutoCloseable {

    private final ExecutorService executor;

    public BackgroundExecutor() {
        this(Math.max(2, Runtime.getRuntime().availableProcessors()));
    }

    public BackgroundExecutor(int poolSize) {
        if (poolSize < 1) {
            throw new IllegalArgumentException("poolSize должен быть >= 1");
        }
        ThreadFactory factory = new ThreadFactory() {
            private final AtomicInteger n = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "rbac-bg-" + n.getAndIncrement());
                t.setDaemon(false);
                return t;
            }
        };
        this.executor = Executors.newFixedThreadPool(poolSize, factory);
    }

    public ExecutorService executor() {
        return executor;
    }

    public void execute(Runnable task) {
        executor.execute(Objects.requireNonNull(task, "task"));
    }

    public <T> Future<T> submit(Callable<T> task) {
        return executor.submit(Objects.requireNonNull(task, "task"));
    }

    public Future<?> submit(Runnable task) {
        return executor.submit(Objects.requireNonNull(task, "task"));
    }

    public boolean isShutdown() {
        return executor.isShutdown();
    }

    public void shutdown() {
        executor.shutdown();
    }

    public void shutdownNow() {
        executor.shutdownNow();
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executor.awaitTermination(timeout, unit);
    }

    public void shutdownAndAwaitTermination(long timeout, TimeUnit unit) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(timeout, unit)) {
                executor.shutdownNow();
                executor.awaitTermination(5, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() {
        shutdownAndAwaitTermination(60, TimeUnit.SECONDS);
    }
}
