package uk.nhs.nhsx.diagnosiskeydist;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrentExecution implements AutoCloseable {
	private static final Logger logger = LogManager.getLogger(ConcurrentExecution.class);

	private final String name;
	private final Duration timeout;
	private final AtomicInteger counter;
	private final long start;
	private final ExecutorService pool;

	public ConcurrentExecution(String name, Duration timeout) {
		this.name = name;
		this.timeout = timeout;
		counter = new AtomicInteger();
		start = System.currentTimeMillis();
		pool = Executors.newFixedThreadPool(15);

		logger.info("Begin: {}", name);
	}

	public void execute(Concurrent c) {
		pool.execute(() -> {
			try {
				c.run();

				counter.incrementAndGet();
			} catch (Exception e) {
				logger.error("Error: " + name + ". Terminating lambda.", e);

				System.exit(-1); //hacky but effective: stop Lambda immediately (e.g. all threads) after in an error in one thread
			}
		});
	}

	@Override
	public void close() throws Exception {
		pool.shutdown();
		boolean terminatedGracefully = pool.awaitTermination(timeout.getSeconds(), TimeUnit.SECONDS);
		if (!terminatedGracefully) {
			logger.error("Error: {}. Timed-out while waiting for executor service to shutdown", name);
			pool.shutdownNow();
			throw new IllegalStateException("Timed-out while waiting for executor service to shutdown");
		} else {
			logger.info("Success: {}. Count={}. Duration={} ms", name, counter.get(), (System.currentTimeMillis() - start));
		}

	}

	public interface Concurrent {
		void run() throws Exception;
	}
}
