package uk.nhs.nhsx.domain

import dev.forkhandles.values.Validation

fun Validation<String>.withMessage(prefix: String) = let {
    { testValue: String -> it(testValue).also { if (!it) throw IllegalArgumentException(prefix) } }
}
