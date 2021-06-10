package uk.nhs.nhsx.diagnosiskeydist

import uk.nhs.nhsx.core.Clock
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.ExceptionThrown
import uk.nhs.nhsx.core.events.InfoEvent
import uk.nhs.nhsx.core.handler.RequestContext
import uk.nhs.nhsx.diagnosiskeydist.ConcurrentExecution.OnErrorHandler
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.exitProcess

class ConcurrentExecution(
    private val name: String,
    private val timeout: Duration,
    private val events: Events,
    private val clock: Clock,
    private val onErrorHandler: OnErrorHandler
) : AutoCloseable {
    private val counter: AtomicInteger = AtomicInteger()
    private val start: Instant = clock()
    private val pool: ExecutorService = Executors.newFixedThreadPool(15)
    private val existingRequestId: String = RequestContext.awsRequestId()

    fun execute(c: Runnable) {
        pool.execute {
            try {
                RequestContext.assignAwsRequestId(existingRequestId)
                c.run()
                counter.incrementAndGet()
            } catch (e: Exception) {
                events(
                    ExceptionThrown(e, "Error: $name. Terminating lambda (System.exit).")
                )
                onErrorHandler.handle()
            }
        }
    }

    override fun close() {
        pool.shutdown()
        val terminatedGracefully = pool.awaitTermination(timeout.seconds, TimeUnit.SECONDS)
        if (!terminatedGracefully) {
            events(ExecutorTimedOutWaitingForShutdown)
            pool.shutdownNow()
            throw IllegalStateException("Timed-out while waiting for executor service to shutdown")
        } else {
            events(
                InfoEvent("Success: $name. Count=${counter.get()}. Duration=${Duration.between(start, clock())}")
            )
        }
    }

    fun interface OnErrorHandler {
        fun handle()
    }

    companion object {
        val SYSTEM_EXIT_ERROR_HANDLER: OnErrorHandler = OnErrorHandler {
            exitProcess(-1) //hacky but effective: stop Lambda immediately (e.g. all threads) after in an error in one thread
        }
    }
}
