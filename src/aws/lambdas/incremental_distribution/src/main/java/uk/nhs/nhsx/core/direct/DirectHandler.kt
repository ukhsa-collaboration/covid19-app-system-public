package uk.nhs.nhsx.core.direct

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.fasterxml.jackson.core.type.TypeReference
import uk.nhs.nhsx.core.Handler
import uk.nhs.nhsx.core.StandardHandlers.logAndRethrowException
import uk.nhs.nhsx.core.SystemObjectMapper.MAPPER
import uk.nhs.nhsx.core.events.Event
import uk.nhs.nhsx.core.events.EventCategory
import uk.nhs.nhsx.core.events.Events
import java.time.Duration

abstract class DirectHandler<T, R>(
    protected val events: Events,
    private val clazz: Class<T>
) : RequestHandler<Map<String, Any>, Map<String, Any>> {

    override fun handleRequest(request: Map<String, Any>, context: Context): Map<String, Any> {
        val start = System.currentTimeMillis()
        events(DirectRequestStarted(javaClass.simpleName))

        return logAndRethrowException(events, convertExceptionsToLambdaError(handler(), clazz))(request, context)
            .also {
                events(
                    DirectRequestCompleted(javaClass.simpleName, Duration.ofMillis(System.currentTimeMillis() - start))
                )
            }
    }

    abstract fun handler(): Handler<T, R>
}

data class DirectRequestStarted(val handler: String) : Event(EventCategory.Metric)
data class DirectRequestCompleted(val handler: String, val runtime: Duration) : Event(EventCategory.Metric)


fun <T, R> convertExceptionsToLambdaError(handler: Handler<T, R>, clazz: Class<T>) =
    Handler<Map<String, Any>, Map<String, Any>> { req, ctx ->
        MAPPER.convertValue(
            handler(MAPPER.convertValue(req, clazz), ctx),
            object : TypeReference<Map<String, String>>() {})
    }
