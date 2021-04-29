package uk.nhs.nhsx.diagnosiskeydist

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.SystemClock
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.handler.RequestContext
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
        latch.await(5, TimeUnit.SECONDS)
        assertThat(captured).isEqualTo("hello")
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
