package smoke

import assertions.CtaExchangeAssertionsV1.testEndDate
import assertions.CtaExchangeAssertionsV1.testKit
import assertions.CtaExchangeAssertionsV1.testResult
import assertions.CtaExchangeAssertionsV2.diagnosisKeySubmissionSupported
import assertions.CtaExchangeAssertionsV2.requiresConfirmatoryTest
import assertions.CtaExchangeAssertionsV2.testEndDate
import assertions.CtaExchangeAssertionsV2.testKit
import assertions.CtaExchangeAssertionsV2.testResult
import assertions.VirologyAssertionsV1.testEndDate
import assertions.VirologyAssertionsV1.testKit
import assertions.VirologyAssertionsV1.testResult
import assertions.VirologyAssertionsV2.diagnosisKeySubmissionSupported
import assertions.VirologyAssertionsV2.requiresConfirmatoryTest
import assertions.VirologyAssertionsV2.testEndDate
import assertions.VirologyAssertionsV2.testKit
import assertions.VirologyAssertionsV2.testResult
import org.http4k.cloudnative.env.Environment
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import smoke.actors.ApiVersion.V1
import smoke.actors.ApiVersion.V2
import smoke.actors.MobileApp
import smoke.actors.TestLab
import smoke.actors.createHandler
import smoke.env.SmokeTests
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import uk.nhs.nhsx.domain.Country.Companion.England
import uk.nhs.nhsx.domain.TestEndDate
import uk.nhs.nhsx.domain.TestKit
import uk.nhs.nhsx.domain.TestKit.RAPID_RESULT
import uk.nhs.nhsx.domain.TestKit.RAPID_SELF_REPORTED
import uk.nhs.nhsx.domain.TestResult.Positive
import uk.nhs.nhsx.virology.VirologyUploadHandler.VirologyResultSource.Npex
import uk.nhs.nhsx.virology.VirologyUploadHandler.VirologyTokenExchangeSource.Eng
import uk.nhs.nhsx.virology.exchange.CtaExchangeResult
import uk.nhs.nhsx.virology.lookup.VirologyLookupResult

class VirologyMixSmokeTest {

    private val client = createHandler(Environment.ENV)
    private val config = SmokeTests.loadConfig()
    private val mobileApp = MobileApp(client, config)
    private val testLab = TestLab(client, config)

    @ParameterizedTest
    @EnumSource(value = TestKit::class)
    fun `app orders from v2, lab uploads on v1, app polls on v2`(input: TestKit) {
        val orderResponse = mobileApp.orderTest(V2)

        testLab.uploadTestResult(
            token = orderResponse.tokenParameterValue,
            result = Positive,
            testKit = input,
            source = Npex,
            apiVersion = V1
        )

        val pollingToken = orderResponse.testResultPollingToken

        val response = mobileApp.pollForTestResult(pollingToken, V2)

        expectThat(response)
            .isA<VirologyLookupResult.AvailableV2>()
            .get(VirologyLookupResult.AvailableV2::response).and {
                testResult.isEqualTo(Positive)
                testKit.isEqualTo(TestKit.LAB_RESULT)
                testEndDate.isEqualTo(TestEndDate.of(2020, 4, 23))
                diagnosisKeySubmissionSupported.isTrue()
                requiresConfirmatoryTest.isFalse()
            }
    }

    @ParameterizedTest
    @EnumSource(value = TestKit::class, names = ["LAB_RESULT", "RAPID_RESULT"])
    fun `app orders from v1, lab uploads on v2, app polls on v1`(input: TestKit) {
        val orderResponse = mobileApp.orderTest(V1)

        testLab.uploadTestResult(
            token = orderResponse.tokenParameterValue,
            result = Positive,
            testKit = input,
            source = Npex,
            apiVersion = V2
        )

        val pollingToken = orderResponse.testResultPollingToken
        val response = mobileApp.pollForTestResult(pollingToken, V1)

        expectThat(response)
            .isA<VirologyLookupResult.AvailableV1>()
            .get(VirologyLookupResult.AvailableV1::response).and {
                testResult.isEqualTo(Positive)
                testKit.isEqualTo(input)
                testEndDate.isEqualTo(TestEndDate.of(2020, 4, 23))
            }
    }

    @Test
    fun `app orders from v1, lab uploads self reported test on v2, app polls on v1 is pending, polls on v2 is available`() {
        val orderResponse = mobileApp.orderTest(V1)

        testLab.uploadTestResult(
            token = orderResponse.tokenParameterValue,
            result = Positive,
            testKit = RAPID_SELF_REPORTED,
            source = Npex,
            apiVersion = V2
        )

        val pollingToken = orderResponse.testResultPollingToken

        repeat(times = 3) {
            val response = mobileApp.pollForTestResult(pollingToken, V1)
            expectThat(response).isA<VirologyLookupResult.Pending>()
        }

        val responseV2 = mobileApp.pollForTestResult(pollingToken, V2)
        expectThat(responseV2).isA<VirologyLookupResult.AvailableV2>()
    }

    @ParameterizedTest
    @EnumSource(value = TestKit::class)
    fun `lab token gen from v1 and app ctaExchange from v2 via different test kits`(input: TestKit) {
        val ctaToken = testLab.generateCtaTokenFor(
            testResult = Positive,
            testEndDate = TestEndDate.of(2020, 11, 19),
            source = Eng,
            apiVersion = V1,
            testKit = input
        )

        val exchangeResponse = mobileApp.exchange(ctaToken, V2)

        expectThat(exchangeResponse)
            .isA<CtaExchangeResult.AvailableV2>()
            .get(CtaExchangeResult.AvailableV2::ctaExchangeResponse).and {
                testResult.isEqualTo(Positive)
                testEndDate.isEqualTo(TestEndDate.of(2020, 11, 19))
                testKit.isEqualTo(TestKit.LAB_RESULT)
                diagnosisKeySubmissionSupported.isTrue()
                requiresConfirmatoryTest.isFalse()
            }
    }

    @ParameterizedTest
    @EnumSource(value = TestKit::class, names = ["LAB_RESULT", "RAPID_RESULT"])
    fun `lab token gen from v2 and app ctaExchange from v1`(input: TestKit) {
        val ctaToken = testLab.generateCtaTokenFor(
            testResult = Positive,
            testEndDate = TestEndDate.of(2020, 11, 19),
            source = Eng,
            apiVersion = V2,
            testKit = input
        )

        val exchangeResponse = mobileApp.exchange(ctaToken, V1)

        expectThat(exchangeResponse)
            .isA<CtaExchangeResult.AvailableV1>()
            .get(CtaExchangeResult.AvailableV1::ctaExchangeResponse).and {
                testResult.isEqualTo(Positive)
                testKit.isEqualTo(input)
                testEndDate.isEqualTo(TestEndDate.of(2020, 11, 19))
            }
    }

    @Test
    fun `lab token gen for self reported test from v2 and app ctaExchange from v1 is not found, v2 is available`() {
        val ctaToken = testLab.generateCtaTokenFor(
            testResult = Positive,
            testEndDate = TestEndDate.of(2020, 11, 19),
            source = Eng,
            apiVersion = V2,
            testKit = RAPID_SELF_REPORTED
        )

        repeat(times = 3) {
            val exchangeResponse = mobileApp.exchange(ctaToken, V1)
            expectThat(exchangeResponse).isA<CtaExchangeResult.NotFound>()
        }

        val exchangeResponseV2 = mobileApp.exchange(ctaToken, V2)
        expectThat(exchangeResponseV2).isA<CtaExchangeResult.AvailableV2>()
    }

    @Test
    fun `polling token for rapid result test result found in both v1 and v2`() {
        val orderResponse = mobileApp.orderTest(V2)

        testLab.uploadTestResult(
            token = orderResponse.tokenParameterValue,
            result = Positive,
            testKit = RAPID_RESULT,
            source = Npex,
            apiVersion = V2
        )

        val pollingToken = orderResponse.testResultPollingToken

        val response1 = mobileApp.pollForTestResult(pollingToken, V1)
        expectThat(response1).isA<VirologyLookupResult.AvailableV1>()

        val response2 = mobileApp.pollForTestResult(pollingToken, V2, England)
        expectThat(response2).isA<VirologyLookupResult.AvailableV2>()
    }
}
