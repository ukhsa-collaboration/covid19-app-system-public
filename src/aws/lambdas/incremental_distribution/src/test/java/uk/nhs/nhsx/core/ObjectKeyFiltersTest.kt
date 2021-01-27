package uk.nhs.nhsx.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.nhs.nhsx.core.aws.s3.ObjectKey

class ObjectKeyFiltersTest {

    @ParameterizedTest
    @CsvSource(
        value = [
            // federated keys
            "abc,true",
            "abcdef,true",
            "abc-def,true",
            "abc/def,true",
            "ab/def,false",

            // old-style PCR keys
            "123,true",
            "ab,true",
            "abz,true",

            // new style keys
            "mobile/LAB_RESULT/abc,true",
            "mobile/LAB_RESULT/xyz,true",
            "mobile/RAPID_RESULT/abc,false",

            // misc key
            "///,false",
        ]
    )
    fun `filters LAB_RESULT, mobile root, and allowed federated prefixed keys`(input: String, expected: Boolean) {
        val includeKeyWithAbcPrefix = ObjectKeyFilters
            .federated()
            .withPrefixes(listOf("abc"))

        assertThat(includeKeyWithAbcPrefix.test(ObjectKey.of(input)))
            .describedAs("Key of $input")
            .isEqualTo(expected)
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            // federated keys
            "abc,true",
            "abcdef,true",
            "abc-def,true",
            "abc/def,true",
            "ab/def,false",

            // old-style PCR keys
            "123,true",
            "ab,true",
            "abz,true",

            // new style keys
            "mobile/LAB_RESULT/abc,true",
            "mobile/LAB_RESULT/xyz,true",
            "mobile/RAPID_RESULT/abc,true",

            // misc key
            "///,false",
        ]
    )
    fun `filters LAB_RESULT, RAPID_RESULT, mobile root, and allowed federated prefixed keys`(input: String, expected: Boolean) {
        val includeKeyWithAbcPrefix = ObjectKeyFilters
            .batched()
            .withPrefixes(listOf("abc"))

        assertThat(includeKeyWithAbcPrefix.test(ObjectKey.of(input)))
            .describedAs("Key of $input")
            .isEqualTo(expected)
    }
}
