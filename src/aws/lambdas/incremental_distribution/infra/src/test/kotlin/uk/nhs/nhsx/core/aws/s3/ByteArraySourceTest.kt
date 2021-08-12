package uk.nhs.nhsx.core.aws.s3

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class ByteArraySourceTest {

    private val input = ByteArraySource.fromUtf8String("foobar")

    @Test
    fun `ByteArraySource can be equal`() {
        val first = ByteArraySource.fromUtf8String("Hello")
        val second = ByteArraySource.fromUtf8String("Hello")
        expectThat(first).isEqualTo(second)
    }

    @Test
    fun `returns the correct size`() {
        expectThat(input.size).isEqualTo(6)
    }

    @Test
    fun `returns a byte array`() {
        expectThat(input.toArray()).isEqualTo("foobar".toByteArray())
    }

    @Test
    fun `returns a string`() {
        expectThat(input.toUtf8String()).isEqualTo("foobar")
    }

    @Test
    fun `returns an InputStream`() {
        expectThat(input.openStream().readAllBytes()).isEqualTo("foobar".toByteArray())
    }
}
