package uk.nhs.nhsx.diagnosiskeydist;

import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ConcurrentExecutionTest {

    @Test
    public void concurrentExecutionTimeoutThrowsExceptionInMainThread() throws InterruptedException {

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean interrupted = new AtomicBoolean(false);

        assertThatThrownBy(() -> {
            try (ConcurrentExecution pool = new ConcurrentExecution("pool", Duration.ofSeconds(1))) {
                pool.execute(() -> {
                    try {
                        TimeUnit.SECONDS.sleep(10);
                    } catch (InterruptedException e) {
                        interrupted.set(true);
                        latch.countDown();
                    }
                });
            }
        }).isInstanceOf(IllegalStateException.class);

        latch.await(5, TimeUnit.SECONDS);

        assertThat(interrupted.get()).isTrue();
    }

}
