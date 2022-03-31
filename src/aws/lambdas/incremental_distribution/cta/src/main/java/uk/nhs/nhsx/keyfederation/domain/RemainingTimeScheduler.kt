package uk.nhs.nhsx.keyfederation.domain

import com.amazonaws.services.lambda.runtime.Context
import uk.nhs.nhsx.core.Clock
import java.time.Duration

class RemainingTimeScheduler<R>(
    private val context: Context,
    private val clock: Clock
) {
    var maxDuration: Duration = Duration.ZERO

    fun runMaybe(task: () -> R) = when {
        context.remainingTimeInMillis > maxDuration.toMillis() -> {
            val start = clock()
            val response = task()
            val duration = Duration.between(start, clock())
            maxDuration = if (maxDuration > duration) maxDuration else duration
            response
        }
        else -> null
    }
}
