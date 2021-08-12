package uk.nhs.nhsx.testhelper.assertions

import org.skyscreamer.jsonassert.JSONCompare
import org.skyscreamer.jsonassert.JSONCompareMode
import strikt.api.Assertion
import uk.nhs.nhsx.core.Json
import java.io.InputStream

fun Assertion.Builder<String>.isEqualToJson(expected: String) =
    assert("is equal to JSON %s", expected) { subject ->
        val result = JSONCompare.compareJSON(expected, subject, JSONCompareMode.STRICT)
        when {
            result.failed() -> fail(subject, result.message)
            else -> pass(subject)
        }
    }

inline fun <reified T : Any> Assertion.Builder<String>.withReadJsonOrThrow(noinline block: Assertion.Builder<T>.() -> Unit): Assertion.Builder<String> =
    with({ Json.readJsonOrThrow(this) }, block)

inline fun <reified T : Any> Assertion.Builder<String>.readJsonOrThrow() =
    get { Json.readJsonOrThrow<T>(this) }

fun Assertion.Builder<*>.toJson() = get { Json.toJson(this) }
