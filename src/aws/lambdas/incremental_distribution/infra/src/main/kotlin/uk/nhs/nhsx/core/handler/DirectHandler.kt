package uk.nhs.nhsx.core.handler

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestStreamHandler
import uk.nhs.nhsx.core.Handler
import uk.nhs.nhsx.core.Json
import uk.nhs.nhsx.core.events.Event
import uk.nhs.nhsx.core.events.EventCategory
import uk.nhs.nhsx.core.events.Events
import java.io.InputStream
import java.io.OutputStream
import java.time.Duration
import kotlin.reflect.KClass

abstract class DirectHandler<T : Any, R>(
    protected val events: Events,
    private val clazz: KClass<T>
) : RequestStreamHandler {
    override fun handleRequest(request: InputStream, output: OutputStream, context: Context) {
        RequestContext.assignAwsRequestId(context.awsRequestId)

        val start = System.currentTimeMillis()
        events(DirectRequestStarted(javaClass.simpleName))

        logAndRethrowException(events, convertExceptionsToLambdaError(handler(), clazz))(request, context)
            .also {
                events(
                    DirectRequestCompleted(javaClass.simpleName, Duration.ofMillis(System.currentTimeMillis() - start))
                )
            }
            .copyTo(output)
    }

    abstract fun handler(): Handler<T, R>
}

data class DirectRequestStarted(val handler: String) : Event(EventCategory.Metric)
data class DirectRequestCompleted(val handler: String, val runtime: Duration) : Event(EventCategory.Metric)

fun <T : Any, R> convertExceptionsToLambdaError(handler: Handler<T, R>, clazz: KClass<T>) =
    Handler<InputStream, InputStream> { req, ctx ->
        Json.toJson(handler(Json.readJsonOrThrow(req, clazz), ctx)).byteInputStream()
    }

