package uk.nhs.nhsx.testhelper.assertions

import strikt.api.Assertion
import strikt.assertions.isEqualTo
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

object InputAssertions {
    fun Assertion.Builder<InputStream>.hasSameContentAs(expected: String) =
        get(InputStream::readAllBytes).get(::String).isEqualTo(expected)
}

object OutputAssertions {
    fun Assertion.Builder<ByteArrayOutputStream>.hasSameContentAs(expected: String) =
        get(ByteArrayOutputStream::toByteArray).get(::String).isEqualTo(expected)
}


