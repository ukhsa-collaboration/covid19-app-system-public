package uk.nhs.nhsx.core

import org.assertj.core.api.Assertions.*
import org.junit.Test
import uk.nhs.nhsx.core.UncheckedException.uncheckedGet
import java.net.MalformedURLException
import java.net.URL

class UncheckedExceptionTest {

    @Test
    fun `converts checked into unchecked`() {
        assertThatThrownBy { uncheckedGet { throw NullPointerException("something is null") } }
            .isInstanceOf(RuntimeException::class.java)
            .hasMessageContaining("something is null")
            .hasCauseInstanceOf(NullPointerException::class.java)
    }

    @Test
    fun `keeps unchecked`() {
        assertThatThrownBy { uncheckedGet { throw IllegalStateException("123") } }
            .isInstanceOf(RuntimeException::class.java)
            .hasMessageContaining("123")
            .hasCauseInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `returns result without throwing`() {
        val url = uncheckedGet { URL("http://www.example.com") }
        assertThat(url.toString()).isEqualTo("http://www.example.com")
    }

    @Test
    fun `throws unchecked on invalid url`() {
        assertThatThrownBy { uncheckedGet { URL("abc") } }
            .isInstanceOf(RuntimeException::class.java)
            .hasCauseInstanceOf(MalformedURLException::class.java)
    }
}