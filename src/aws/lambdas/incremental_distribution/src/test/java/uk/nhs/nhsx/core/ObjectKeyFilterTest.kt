package uk.nhs.nhsx.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ObjectKeyFilterTest {

    @Test
    fun `filters mobile keys and allowed prefixes`() {
        val excludeKeyWithPrefix = ObjectKeyFilter.includeMobileAndAllowedPrefixes(listOf("abc"))

        assertThat(excludeKeyWithPrefix.test("abc")).isTrue()
        assertThat(excludeKeyWithPrefix.test("abcdef")).isTrue()
        assertThat(excludeKeyWithPrefix.test("abc-def")).isTrue()
        assertThat(excludeKeyWithPrefix.test("ab/def")).isFalse()
        assertThat(excludeKeyWithPrefix.test("abc/def")).isTrue()

        assertThat(excludeKeyWithPrefix.test("123")).isTrue()
        assertThat(excludeKeyWithPrefix.test("ab")).isTrue()
        assertThat(excludeKeyWithPrefix.test("abz")).isTrue()
        assertThat(excludeKeyWithPrefix.test("///")).isFalse()
    }
}