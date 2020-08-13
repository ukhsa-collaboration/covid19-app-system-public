package uk.nhs.nhsx.diagnosiskeydist;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrentExecution implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(ConcurrentExecution.class);

	private final String name;
	private final long timeoutMinutes;
	private final AtomicInteger counter;
	private final long start;
	private final ExecutorService pool;

	public ConcurrentExecution(String name, long timeoutMinutes) {
		this.name = name;
		this.timeoutMinutes = timeoutMinutes;
		counter = new AtomicInteger();
		start = System.currentTimeMillis();
		pool = Executors.newFixedThreadPool(15);

		logger.info("Begin: {}", name);
	}

	public void execute(Concurrent c) {
		pool.execute(new Runnable() {
			@Override
			public void run() {
				try {
					c.run();

					counter.incrementAndGet();
				} catch (Exception e) {
					logger.error("Error: " + name + ". Terminating lambda.", e);

					System.exit(-1); //hacky but effective: stop Lambda immediately (e.g. all threads) after in an error in one thread
				}
			}
		});
	}

	@Override
	public void close() throws Exception {
		pool.shutdown();
		pool.awaitTermination(timeoutMinutes, TimeUnit.MINUTES);

		logger.info("Success: {}. Count={}. Duration={} ms", name, counter.get(), (System.currentTimeMillis() - start));
	}

	public interface Concurrent {
		public void run() throws Exception;
	}
}
