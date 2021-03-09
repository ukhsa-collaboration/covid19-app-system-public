package uk.nhs.nhsx.diagnosiskeydist

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.apache.logging.log4j.ThreadContext
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.SystemClock
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.diagnosiskeydist.ConcurrentExecution.OnErrorHandler
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class ConcurrentExecutionTest {

    @Test
    fun `concurrent execution timeout throws exception in main thread`() {
        val latch = CountDownLatch(1)
        val interrupted = AtomicBoolean(false)
        assertThatThrownBy {
            concurrentExecution().use { pool ->
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

    @Test
    fun `copies MDC context onto new threads`() {
        assertThat(ThreadContext.containsKey("message")).isFalse

        try {
            ThreadContext.put("message", "hello")
            val context = ThreadContext.getImmutableContext()
            var capturedContext: Map<String, String> = mapOf()

            val latch = CountDownLatch(1)
            concurrentExecution().use { pool ->
                pool.execute {
                    capturedContext = ThreadContext.getImmutableContext()
                    latch.countDown()
                }
            }
            latch.await(5, TimeUnit.SECONDS)
            assertThat(context.hashCode()).isNotSameAs(capturedContext.hashCode())
            assertThat(capturedContext["message"]).isEqualTo("hello")
        } finally {
            ThreadContext.remove("counter")
        }
    }

    @Test
    fun `calls onErrorHandler`() {
        val onErrorHandler = mockk<OnErrorHandler>()
        every { onErrorHandler.handle() } returns Unit

        val latch = CountDownLatch(1)
        concurrentExecution(onErrorHandler).use { pool ->
            pool.execute { error("Oh no!") }
        }
        latch.await(5, TimeUnit.SECONDS)

        verify(exactly = 1) { onErrorHandler.handle() }
    }

    private fun concurrentExecution(onError: OnErrorHandler = OnErrorHandler { }) =
        ConcurrentExecution("pool", Duration.ofSeconds(1), RecordingEvents(), SystemClock.CLOCK, onError)
}
