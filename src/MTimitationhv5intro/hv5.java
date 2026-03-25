package MTimitationhv5intro;

import java.util.concurrent.locks.ReentrantLock;

class MyRunnable implements Runnable {
    private final int runtime;
    private final int threadNumber;
    private static final ReentrantLock printLock = new ReentrantLock();

    public MyRunnable(int runtime, int threadNumber) {
        this.runtime = runtime;
        this.threadNumber = threadNumber;
    }

    private void safePrint(String message) {
        printLock.lock();
        try {
            System.out.println(message);
        } finally {
            printLock.unlock();
        }
    }

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        long threadId = Thread.currentThread().getId();

        safePrint("Thread " + threadNumber + " (ID: " + threadId + ") started");

        for (int i = 0; i < runtime; i++) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }

            StringBuilder bar = new StringBuilder();
            bar.append("Thread ").append(threadNumber).append(" (ID: ").append(threadId)
                    .append("): [");
            for (int j = 0; j <= i; j++) bar.append('o');
            for (int j = i + 1; j < runtime; j++) bar.append(' ');
            bar.append("] ").append(i + 1).append("/").append(runtime);

            safePrint(bar.toString());
        }

        long duration = System.currentTimeMillis() - startTime;
        safePrint("Thread " + threadNumber + " (ID: " + threadId +
                ") completed task in " + duration + " ms");
        safePrint("");
    }
}

public class hv5 {
    public static void main(String[] args) {
        final int THREAD_COUNT = 5;
        final int RUNTIME = 10;

        System.out.println("Threads: " + THREAD_COUNT + ", runtime: " + RUNTIME + " seconds");
        System.out.println();

        for (int i = 1; i <= THREAD_COUNT; i++) {
            new Thread(new MyRunnable(RUNTIME, i), "Thread-" + i).start();
        }
    }
}
