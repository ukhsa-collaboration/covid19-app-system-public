package uk.nhs.nhsx.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ObjectKeyFilterTest {

    @Test
    fun filterKeyWithPrefix() {

        val excludeKeyWithPrefix = ObjectKeyFilter.excludeKeyWithPrefix("abc")

        assertThat(excludeKeyWithPrefix.test("abc")).isFalse()
        assertThat(excludeKeyWithPrefix.test("abcdef")).isFalse()
        assertThat(excludeKeyWithPrefix.test("abc-def")).isFalse()
        assertThat(excludeKeyWithPrefix.test("abc/def")).isFalse()

        assertThat(excludeKeyWithPrefix.test("123")).isTrue()
        assertThat(excludeKeyWithPrefix.test("ab")).isTrue()
        assertThat(excludeKeyWithPrefix.test("abz")).isTrue()
        assertThat(excludeKeyWithPrefix.test("///")).isTrue()

    }
}