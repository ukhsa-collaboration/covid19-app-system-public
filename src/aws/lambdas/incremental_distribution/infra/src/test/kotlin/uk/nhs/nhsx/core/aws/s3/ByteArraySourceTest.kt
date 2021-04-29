package uk.nhs.nhsx.core.aws.s3

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream

class ByteArraySourceTest {

    private val input = ByteArraySource.fromUtf8String("foobar")

    @Test
    fun `ByteArraySource can be equal`() {
        val first = ByteArraySource.fromUtf8String("Hello")
        val second = ByteArraySource.fromUtf8String("Hello")
        assertThat(first).isEqualTo(second)
    }

    @Test
    fun `returns the correct size`() {
        assertThat(input.size)
            .isEqualTo(6)
    }

    @Test
    fun `returns a byte array`() {
        assertThat(input.toArray())
            .isEqualTo("foobar".toByteArray())
    }

    @Test
    fun `returns a string`() {
        assertThat(input.toUtf8String())
            .isEqualTo("foobar")
    }

    @Test
    fun `returns an InputStream`() {
        assertThat(input.openStream())
            .hasSameContentAs(ByteArrayInputStream("foobar".toByteArray()))
    }
}
