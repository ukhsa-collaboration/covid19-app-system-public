package uk.nhs.nhsx.diagnosiskeydist

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue
import uk.nhs.nhsx.core.SystemClock
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.handler.RequestContext
import uk.nhs.nhsx.diagnosiskeydist.ConcurrentExecution.OnErrorHandler
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicBoolean

class ConcurrentExecutionTest {

    @Test
    fun `concurrent execution timeout throws exception in main thread`() {
        val latch = CountDownLatch(1)
        val interrupted = AtomicBoolean(false)

        expectThrows<IllegalStateException> {
            concurrentExecution().use { pool ->
                pool.execute {
                    try {
                        SECONDS.sleep(10)
                    } catch (e: InterruptedException) {
                        interrupted.set(true)
                        latch.countDown()
                    }
                }
            }
        }

        latch.await(5, SECONDS)

        expectThat(interrupted.get()).isTrue()
    }

    @Test
    fun `copies request context onto new threads`() {
        RequestContext.assignAwsRequestId("hello")
        var captured = ""

        val latch = CountDownLatch(1)
        concurrentExecution().use { pool ->
            pool.execute {
                captured = RequestContext.awsRequestId()
                latch.countDown()
            }
        }
        latch.await(5, SECONDS)
        expectThat(captured).isEqualTo("hello")
    }

    @Test
    fun `calls onErrorHandler`() {
        val onErrorHandler = mockk<OnErrorHandler>()
        every { onErrorHandler.handle() } returns Unit

        val latch = CountDownLatch(1)
        concurrentExecution(onErrorHandler).use { pool ->
            pool.execute { error("Oh no!") }
        }
        latch.await(5, SECONDS)

        verify(exactly = 1) { onErrorHandler.handle() }
    }

    private fun concurrentExecution(onError: OnErrorHandler = OnErrorHandler { }) = ConcurrentExecution(
        "pool",
        Duration.ofSeconds(1),
        RecordingEvents(),
        SystemClock.CLOCK,
        onError
    )
}
