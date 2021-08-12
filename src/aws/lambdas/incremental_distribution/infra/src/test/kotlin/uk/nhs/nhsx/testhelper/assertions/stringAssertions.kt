package uk.nhs.nhsx.testhelper.assertions

import dev.forkhandles.values.AbstractValue
import strikt.api.Assertion
import strikt.assertions.isEqualTo
import strikt.assertions.isNullOrBlank

fun Assertion.Builder<String?>.isNotNullOrBlank() = not().isNullOrBlank()

inline fun <reified T : AbstractValue<String>> Assertion.Builder<String>.isSameAs(expected: T) =
    isEqualTo(expected.value)
