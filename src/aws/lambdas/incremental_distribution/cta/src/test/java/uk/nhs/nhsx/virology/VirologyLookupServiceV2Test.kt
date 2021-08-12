@file:Suppress("TestFunctionName")

package uk.nhs.nhsx.virology

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verifySequence
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import uk.nhs.nhsx.core.events.RecordingEvents
import uk.nhs.nhsx.core.headers.MobileAppVersion
import uk.nhs.nhsx.domain.Country
import uk.nhs.nhsx.domain.Country.Companion.England
import uk.nhs.nhsx.domain.TestKit
import uk.nhs.nhsx.domain.TestKit.LAB_RESULT
import uk.nhs.nhsx.domain.TestKit.RAPID_SELF_REPORTED
import uk.nhs.nhsx.domain.TestResultPollingToken
import uk.nhs.nhsx.testhelper.data.TestData
import uk.nhs.nhsx.virology.lookup.VirologyLookupRequestV2
import uk.nhs.nhsx.virology.lookup.VirologyLookupResponseV2
import uk.nhs.nhsx.virology.lookup.VirologyLookupResult.AvailableV2
import uk.nhs.nhsx.virology.lookup.VirologyLookupResult.NotFound
import uk.nhs.nhsx.virology.lookup.VirologyLookupResult.Pending
import uk.nhs.nhsx.virology.lookup.VirologyLookupService
import uk.nhs.nhsx.virology.persistence.VirologyDataTimeToLive
import uk.nhs.nhsx.virology.persistence.VirologyPersistenceService
import uk.nhs.nhsx.virology.policy.VirologyPolicyConfig
import java.time.Duration
import java.time.Instant
import java.util.*

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
        every { persistence.markForDeletion(any(), any()) } just runs

        val service = VirologyLookupService()
        val request = VirologyLookupRequestV2(pollingToken, country)
        val lookupResult = service.lookup(request, mobileAppVersion)

        verifySequence {
            persistence.getTestResult(pollingToken)
            persistence.markForDeletion(
                testResult, VirologyDataTimeToLive(
                    now.plus(Duration.ofHours(4)),
                    now.plus(Duration.ofDays(4))
                )
            )
        }

        expectThat(lookupResult).isA<AvailableV2>().and {
            with(AvailableV2::response) {
                get(VirologyLookupResponseV2::testEndDate).isEqualTo(testResult.testEndDate)
                get(VirologyLookupResponseV2::testResult).isEqualTo(testResult.testResult)
                get(VirologyLookupResponseV2::testKit).isEqualTo(LAB_RESULT)
                get(VirologyLookupResponseV2::diagnosisKeySubmissionSupported).isTrue()
                get(VirologyLookupResponseV2::requiresConfirmatoryTest).isFalse()
            }
        }
    }

    @ParameterizedTest
    @EnumSource(TestKit::class)
    fun `lookup negative lab result returns correct response flags`(testKit: TestKit) {
        val testResult = TestData.negativeResultFor(testKit)

        every { persistence.getTestResult(any()) } returns Optional.of(testResult)
        every { persistence.markForDeletion(any(), any()) } just runs

        val service = VirologyLookupService()
        val request = VirologyLookupRequestV2(pollingToken, country)
        val result = service.lookup(request, mobileAppVersion)

        expectThat(result).isA<AvailableV2>().and {
            with(AvailableV2::response) {
                get(VirologyLookupResponseV2::diagnosisKeySubmissionSupported).isFalse()
                get(VirologyLookupResponseV2::requiresConfirmatoryTest).isFalse()
            }
        }
    }

    @ParameterizedTest
    @EnumSource(TestKit::class)
    fun `lookup void lab result returns correct response flags`(testKit: TestKit) {
        val testResult = TestData.voidResultFor(testKit)

        every { persistence.getTestResult(any()) } returns Optional.of(testResult)
        every { persistence.markForDeletion(any(), any()) } just runs

        val service = VirologyLookupService()
        val request = VirologyLookupRequestV2(pollingToken, country)
        val result = service.lookup(request, mobileAppVersion)

        expectThat(result).isA<AvailableV2>().and {
            with(AvailableV2::response) {
                get(VirologyLookupResponseV2::diagnosisKeySubmissionSupported).isFalse()
                get(VirologyLookupResponseV2::requiresConfirmatoryTest).isFalse()
            }
        }
    }

    @ParameterizedTest
    @CsvSource("England,true", "Wales,true", "random,false")
    fun `lookup supporting diagnosis keys submission for each country`(country: String, expectedFlag: Boolean) {
        val testResult = TestData.positiveLabResult

        every { persistence.getTestResult(any()) } returns Optional.of(testResult)
        every { persistence.markForDeletion(any(), any()) } just runs

        val service = VirologyLookupService()
        val request = VirologyLookupRequestV2(pollingToken, Country.of(country))
        val result = service.lookup(request, mobileAppVersion)

        expectThat(result).isA<AvailableV2>().and {
            with(AvailableV2::response) {
                get(VirologyLookupResponseV2::diagnosisKeySubmissionSupported).isEqualTo(expectedFlag)
            }
        }
    }

    @ParameterizedTest
    @CsvSource("England,false", "Wales,false", "random,false")
    fun `lookup requesting confirmatory test for each country`(country: String, expectedFlag: Boolean) {
        val testResult = TestData.positiveResultFor(pollingToken, RAPID_SELF_REPORTED)

        every { persistence.getTestResult(pollingToken) } returns Optional.of(testResult)
        every { persistence.markForDeletion(any(), any()) } just runs

        val service = VirologyLookupService()
        val request = VirologyLookupRequestV2(pollingToken, Country.of(country))
        val result = service.lookup(request, mobileAppVersion)

        expectThat(result).isA<AvailableV2>().and {
            with(AvailableV2::response) {
                get(VirologyLookupResponseV2::requiresConfirmatoryTest).isEqualTo(expectedFlag)
            }
        }
    }

    @Test
    fun `lookup with result pending`() {
        every { persistence.getTestResult(any()) } returns Optional.of(TestData.pendingTestResult)

        val service = VirologyLookupService()
        val request = VirologyLookupRequestV2(pollingToken, country)
        val lookupResult = service.lookup(request, mobileAppVersion)

        expectThat(lookupResult).isA<Pending>()
    }

    @Test
    fun `lookup with no match`() {
        every { persistence.getTestResult(any()) } returns Optional.empty()

        val service = VirologyLookupService()
        val request = VirologyLookupRequestV2(pollingToken, country)
        val lookupResult = service.lookup(request, mobileAppVersion)

        expectThat(lookupResult).isA<NotFound>()
    }

    private fun VirologyLookupService() = VirologyLookupService(
        persistence = persistence,
        clock = clock,
        policyConfig = virologyPolicyConfig,
        events = RecordingEvents()
    )
}
