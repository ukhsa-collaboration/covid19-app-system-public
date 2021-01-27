package uk.nhs.nhsx.virology.lookup

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.virology.Country
import uk.nhs.nhsx.virology.CountryTestKitWhitelist
import uk.nhs.nhsx.virology.CountryTestKitWhitelist.CountryTestKitPair.of
import uk.nhs.nhsx.virology.TestKit

class CountryTestKitWhitelistTest {

    @Test
    fun `supports all`() {
        val testCases = listOf(
            of(Country.of("England"), TestKit.LAB_RESULT),
            of(Country.of("England"), TestKit.RAPID_RESULT),
            of(Country.of("Wales"), TestKit.LAB_RESULT),
            of(Country.of("Wales"), TestKit.RAPID_RESULT)
        )

        testCases.forEach {
            assertThat(CountryTestKitWhitelist.isDiagnosisKeysSubmissionSupported(it)).isTrue
        }
    }

    @Test
    fun `blocks all`() {
        val testCases = listOf(
            of(Country.of("Some-Country"), TestKit.RAPID_RESULT),
            of(Country.of("Some-Country"), TestKit.LAB_RESULT)
        )

        testCases.forEach {
            assertThat(CountryTestKitWhitelist.isDiagnosisKeysSubmissionSupported(it)).isFalse
        }
    }
}
