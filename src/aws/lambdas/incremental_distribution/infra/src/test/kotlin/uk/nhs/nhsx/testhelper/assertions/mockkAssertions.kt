package uk.nhs.nhsx.testhelper.assertions

import io.mockk.CapturingSlot
import strikt.api.Assertion

fun <T : Any> Assertion.Builder<CapturingSlot<T>>.isCaptured(): Assertion.Builder<CapturingSlot<T>> =
    assertThat("captured a value", CapturingSlot<T>::isCaptured)

val <T : Any> Assertion.Builder<CapturingSlot<T>>.captured: Assertion.Builder<T>
    get() = get("captured value %s") { if (isCaptured) captured else error("No value has been captured") }

fun <T : Any> Assertion.Builder<CapturingSlot<T>>.withCaptured(
    block: Assertion.Builder<T>.() -> Unit
): Assertion.Builder<CapturingSlot<T>> =
    with("captured value %s", CapturingSlot<T>::captured, block)
