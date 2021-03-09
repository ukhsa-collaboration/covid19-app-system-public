package uk.nhs.nhsx.diagnosiskeydist;

import org.apache.logging.log4j.ThreadContext;
import uk.nhs.nhsx.core.events.Events;
import uk.nhs.nhsx.core.events.ExceptionThrown;
import uk.nhs.nhsx.core.events.InfoEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static java.lang.String.format;

public class ConcurrentExecution implements AutoCloseable {

    public static final OnErrorHandler SYSTEM_EXIT_ERROR_HANDLER = () -> {
        System.exit(-1); //hacky but effective: stop Lambda immediately (e.g. all threads) after in an error in one thread
    };

    private final String name;
    private final Duration timeout;
    private final Events events;
    private final Supplier<Instant> clock;
    private final OnErrorHandler onErrorHandler;
    private final AtomicInteger counter;
    private final Instant start;
    private final ExecutorService pool;
    private final Map<String, String> currentContext;

    public ConcurrentExecution(String name,
                               Duration timeout,
                               Events events,
                               Supplier<Instant> clock,
                               OnErrorHandler onErrorHandler) {
        this.name = name;
        this.timeout = timeout;
        this.events = events;
        this.clock = clock;
        this.onErrorHandler = onErrorHandler;
        this.start = clock.get();
        this.counter = new AtomicInteger();
        this.pool = Executors.newFixedThreadPool(15);
        this.currentContext = ThreadContext.getImmutableContext();
    }

    public void execute(Concurrent c) {
        pool.execute(() -> {
            try {
                ThreadContext.putAll(currentContext);
                c.run();
                counter.incrementAndGet();
            } catch (Exception e) {
                events.emit(getClass(), new ExceptionThrown<>(e, format("Error: %s. Terminating lambda (System.exit).", name)));
                onErrorHandler.handle();
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
            events.emit(getClass(), new InfoEvent(format("Success: %s. Count=%d. Duration=%s", name, counter.get(), Duration.between(start, clock.get()))));
        }
    }

    public interface Concurrent {
        void run() throws Exception;
    }

    public interface OnErrorHandler {
        void handle();
    }
}
