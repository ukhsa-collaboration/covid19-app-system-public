package uk.nhs.nhsx.diagnosiskeydist;

import uk.nhs.nhsx.core.events.Events;
import uk.nhs.nhsx.core.events.ExceptionThrown;
import uk.nhs.nhsx.core.events.InfoEvent;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.format;

public class ConcurrentExecution implements AutoCloseable {
    private final String name;
    private final Duration timeout;
    private final Events events;
    private final AtomicInteger counter;
    private final long start;
    private final ExecutorService pool;

    public ConcurrentExecution(String name, Duration timeout, Events events) {
        this.name = name;
        this.timeout = timeout;
        this.events = events;
        this.counter = new AtomicInteger();
        this.start = System.currentTimeMillis();
        this.pool = Executors.newFixedThreadPool(15);
    }

    public void execute(Concurrent c) {
        pool.execute(() -> {
            try {
                c.run();

                counter.incrementAndGet();
            } catch (Exception e) {
                ExceptionThrown<Exception> event = new ExceptionThrown<>(e, "Error: " + name + ". Terminating lambda.");
                events.emit(getClass(), event);

                System.exit(-1); //hacky but effective: stop Lambda immediately (e.g. all threads) after in an error in one thread
            }
        });
    }

    @Override
    public void close() throws Exception {
        pool.shutdown();
        boolean terminatedGracefully = pool.awaitTermination(timeout.getSeconds(), TimeUnit.SECONDS);
        if (!terminatedGracefully) {
            events.emit(getClass(), ExecutorTimedOutWaitingForShutdown.INSTANCE);
            pool.shutdownNow();
            throw new IllegalStateException("Timed-out while waiting for executor service to shutdown");
        } else {
            events.emit(getClass(), new InfoEvent(format("Success: %s. Count= %d. Duration=%d", name, counter.get(), System.currentTimeMillis() - start)));
        }
    }

    public interface Concurrent {
        void run() throws Exception;
    }
}
