package uk.nhs.nhsx.diagnosiskeydist

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.events.RecordingEvents
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class ConcurrentExecutionTest {

    @Test
    fun concurrentExecutionTimeoutThrowsExceptionInMainThread() {
        val latch = CountDownLatch(1)
        val interrupted = AtomicBoolean(false)
        assertThatThrownBy {
            ConcurrentExecution("pool", Duration.ofSeconds(1), RecordingEvents()).use { pool ->
                pool.execute {
                    try {
                        TimeUnit.SECONDS.sleep(10)
                    } catch (e: InterruptedException) {
                        interrupted.set(true)
                        latch.countDown()
                    }
                }
            }
        }.isInstanceOf(IllegalStateException::class.java)
        latch.await(5, TimeUnit.SECONDS)
        assertThat(interrupted.get()).isTrue
    }
}
