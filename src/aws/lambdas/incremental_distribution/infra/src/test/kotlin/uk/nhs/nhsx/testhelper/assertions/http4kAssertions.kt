package uk.nhs.nhsx.testhelper.assertions

import dev.forkhandles.values.AbstractValue
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import strikt.api.Assertion
import strikt.assertions.contains
import strikt.assertions.containsExactly
import strikt.assertions.isEmpty
import strikt.assertions.map
import uk.nhs.nhsx.core.events.RecordingEvents
import kotlin.reflect.KClass

fun Assertion.Builder<Response>.hasStatus(expected: Status) =
    assertThat("has status %s", expected) {
        it.status == expected
    }

fun Assertion.Builder<Response>.header(name: String) =
    get("Header $name") { header(name) }

val Assertion.Builder<Response>.signatureHeader
    get() = header("X-Amz-Meta-Signature")

val Assertion.Builder<Response>.signatureDateHeader
    get() = header("X-Amz-Meta-Signature-Date")

val Assertion.Builder<Response>.body get() = get(Response::body)
val Assertion.Builder<Response>.bodyString get() = get(Response::bodyString)

val Assertion.Builder<RecordingEvents>.events get() = get("events") { iterator().asSequence().toList() }

fun Assertion.Builder<RecordingEvents>.containsExactly(vararg expectedEvents: KClass<*>) = apply {
    events.map { it::class }.containsExactly(expectedEvents.toList())
}

fun Assertion.Builder<RecordingEvents>.contains(vararg expectedEvents: KClass<*>) = apply {
    events.map { it::class }.contains(expectedEvents.toList())
}

fun Assertion.Builder<RecordingEvents>.isEmpty() {
    events.isEmpty()
}

fun <R : Any, T> Assertion.Builder<T>.isSameAs(expected: R) where T : AbstractValue<R> =
    assert("is equal to %s", expected) {
        when (it.value) {
            expected -> pass(actual = it)
            else -> fail(actual = it)
        }
    }

fun Assertion.Builder<Int>.isSameAs(status: Status) =
    assert("is equal to %s", status) {
        when (it) {
            status.code -> pass(actual = it)
            else -> fail(actual = it)
        }
    }

fun Assertion.Builder<String>.isSameAs(method: Method) =
    assert("is equal to %s", method) {
        when (it) {
            method.name -> pass(actual = it)
            else -> fail(actual = it)
        }
    }
