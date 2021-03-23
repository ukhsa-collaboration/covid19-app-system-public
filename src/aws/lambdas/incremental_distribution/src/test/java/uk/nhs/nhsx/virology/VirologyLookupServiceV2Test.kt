package uk.nhs.nhsx.virology

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verifySequence
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.headers.MobileAppVersion
import uk.nhs.nhsx.testhelper.data.TestData
import uk.nhs.nhsx.virology.Country.Companion.England
import uk.nhs.nhsx.virology.TestKit.LAB_RESULT
import uk.nhs.nhsx.virology.TestKit.RAPID_SELF_REPORTED
import uk.nhs.nhsx.virology.lookup.VirologyLookupRequestV2
import uk.nhs.nhsx.virology.lookup.VirologyLookupResult
import uk.nhs.nhsx.virology.lookup.VirologyLookupService
import uk.nhs.nhsx.virology.persistence.VirologyDataTimeToLive
import uk.nhs.nhsx.virology.persistence.VirologyPersistenceService
import java.time.Duration
import java.time.Instant
import java.util.Optional

class VirologyLookupServiceV2Test {

    private val now = Instant.EPOCH
    private val clock = { now }
    private val persistence = mockk<VirologyPersistenceService>()
    private val pollingToken = TestResultPollingToken.of("98cff3dd-882c-417b-a00a-350a205378c7")
    private val country = England
    private val virologyPolicyConfig = VirologyPolicyConfig()
    private val mobileAppVersion = MobileAppVersion.Version(5, 0)

    @Test
    fun `lookup with result available`() {
        val testResult = TestData.positiveLabResult
        every { persistence.getTestResult(any()) } returns Optional.of(testResult)
        every { persistence.markForDeletion(any(), any()) } just Runs
        val service = virologyLookup()
        val request = VirologyLookupRequestV2(pollingToken, country)
        val lookupResult = service.lookup(request, mobileAppVersion)

        val resultAvailable = lookupResult as VirologyLookupResult.AvailableV2
        assertThat(resultAvailable.response.testEndDate).isEqualTo(testResult.testEndDate)
        assertThat(resultAvailable.response.testResult).isEqualTo(testResult.testResult)
        assertThat(resultAvailable.response.testKit).isEqualTo(LAB_RESULT)
        assertThat(resultAvailable.response.diagnosisKeySubmissionSupported).isTrue
        assertThat(resultAvailable.response.requiresConfirmatoryTest).isFalse

        verifySequence {
            persistence.getTestResult(pollingToken)
            persistence.markForDeletion(
                testResult, VirologyDataTimeToLive(
                    now.plus(Duration.ofHours(4)),
                    now.plus(Duration.ofDays(4))
                )
            )
        }
    }

    @ParameterizedTest
    @EnumSource(TestKit::class)
    fun `lookup negative lab result returns correct response flags`(testKit: TestKit) {
        val testResult = TestData.negativeResultFor(testKit)

        every { persistence.getTestResult(any()) } returns Optional.of(testResult)
        every { persistence.markForDeletion(any(), any()) } just Runs

        val service = virologyLookup()
        val request = VirologyLookupRequestV2(pollingToken, country)
        val result = service.lookup(request, mobileAppVersion) as VirologyLookupResult.AvailableV2

        assertThat(result.response.diagnosisKeySubmissionSupported).isFalse
        assertThat(result.response.requiresConfirmatoryTest).isFalse
    }

    @ParameterizedTest
    @EnumSource(TestKit::class)
    fun `lookup void lab result returns correct response flags`(testKit: TestKit) {
        val testResult = TestData.voidResultFor(testKit)

        every { persistence.getTestResult(any()) } returns Optional.of(testResult)
        every { persistence.markForDeletion(any(), any()) } just Runs

        val service = virologyLookup()
        val request = VirologyLookupRequestV2(pollingToken, country)
        val result = service.lookup(request, mobileAppVersion) as VirologyLookupResult.AvailableV2

        assertThat(result.response.diagnosisKeySubmissionSupported).isFalse
        assertThat(result.response.requiresConfirmatoryTest).isFalse
    }

    @ParameterizedTest
    @CsvSource("England,true", "Wales,true", "random,false")
    fun `lookup supporting diagnosis keys submission for each country`(country: String, expectedFlag: Boolean) {
        val testResult = TestData.positiveLabResult

        every { persistence.getTestResult(any()) } returns Optional.of(testResult)
        every { persistence.markForDeletion(any(), any()) } just Runs

        val service = virologyLookup()
        val request = VirologyLookupRequestV2(pollingToken, Country.of(country))
        val result = service.lookup(request, mobileAppVersion) as VirologyLookupResult.AvailableV2

        assertThat(result.response.diagnosisKeySubmissionSupported).isEqualTo(expectedFlag)
    }

    @ParameterizedTest
    @CsvSource("England,true", "Wales,true", "random,false")
    fun `lookup requesting confirmatory test for each country`(country: String, expectedFlag: Boolean) {
        val testResult = TestData.positiveResultFor(pollingToken, RAPID_SELF_REPORTED)

        every { persistence.getTestResult(any()) } returns Optional.of(testResult)
        every { persistence.markForDeletion(any(), any()) } just Runs

        val service = virologyLookup()
        val request = VirologyLookupRequestV2(pollingToken, Country.of(country))
        val result = service.lookup(request, mobileAppVersion) as VirologyLookupResult.AvailableV2

        assertThat(result.response.requiresConfirmatoryTest).isEqualTo(expectedFlag)
    }

    @Test
    fun `lookup with result pending`() {
        every { persistence.getTestResult(any()) } returns Optional.of(TestData.pendingTestResult)

        val service = virologyLookup()
        val request = VirologyLookupRequestV2(pollingToken, country)
        val lookupResult = service.lookup(request, mobileAppVersion)

        assertThat(lookupResult).isInstanceOf(VirologyLookupResult.Pending::class.java)
    }

    @Test
    fun `lookup pending when mobile version is considered old and confirmatory test required`() {
        val testResult = TestData.positiveResultFor(pollingToken, RAPID_SELF_REPORTED)

        every { persistence.getTestResult(any()) } returns Optional.of(testResult)
        every { persistence.markForDeletion(any(), any()) } just Runs

        val service = virologyLookup()
        val request = VirologyLookupRequestV2(pollingToken, country)
        val result = service.lookup(request, MobileAppVersion.Version(4, 3))

        assertThat(result).isInstanceOf(VirologyLookupResult.Pending::class.java)
    }

    @Test
    fun `lookup with no match`() {
        every { persistence.getTestResult(any()) } returns Optional.empty()

        val service = virologyLookup()
        val request = VirologyLookupRequestV2(pollingToken, country)
        val lookupResult = service.lookup(request, mobileAppVersion)

        assertThat(lookupResult).isInstanceOf(VirologyLookupResult.NotFound::class.java)
    }


    private fun virologyLookup() = VirologyLookupService(persistence, clock, virologyPolicyConfig, RecordingEvents())
}
