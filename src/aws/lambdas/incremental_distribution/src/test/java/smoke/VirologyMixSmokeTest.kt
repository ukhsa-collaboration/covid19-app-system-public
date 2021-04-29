package smoke

import org.assertj.core.api.Assertions.assertThat
import org.http4k.client.JavaHttpClient
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import smoke.actors.ApiVersion.V1
import smoke.actors.ApiVersion.V2
import smoke.actors.MobileApp
import smoke.actors.TestLab
import smoke.env.SmokeTests
import uk.nhs.nhsx.domain.Country.Companion.England
import uk.nhs.nhsx.domain.TestKit
import uk.nhs.nhsx.domain.TestKit.RAPID_RESULT
import uk.nhs.nhsx.domain.TestKit.RAPID_SELF_REPORTED
import uk.nhs.nhsx.virology.VirologyUploadHandler.VirologyResultSource.Npex
import uk.nhs.nhsx.virology.VirologyUploadHandler.VirologyTokenExchangeSource.Eng
import uk.nhs.nhsx.virology.exchange.CtaExchangeResult
import uk.nhs.nhsx.virology.lookup.VirologyLookupResult
import uk.nhs.nhsx.domain.TestEndDate
import uk.nhs.nhsx.domain.TestResult.Positive

class VirologyMixSmokeTest {

    private val client = JavaHttpClient()
    private val config = SmokeTests.loadConfig()
    private val mobileApp = MobileApp(client, config)
    private val testLab = TestLab(client, config)

    @ParameterizedTest
    @EnumSource(value = TestKit::class)
    fun `app orders from v2, lab uploads on v1, app polls on v2`(testKit: TestKit) {
        val orderResponse = mobileApp.orderTest(V2)

        testLab.uploadTestResult(
            token = orderResponse.tokenParameterValue,
            result = Positive,
            testKit = testKit,
            source = Npex,
            apiVersion = V1
        )

        val pollingToken = orderResponse.testResultPollingToken

        val response = (mobileApp.pollForTestResult(pollingToken, V2) as VirologyLookupResult.AvailableV2).response
        assertThat(response.testResult).isEqualTo(Positive)
        assertThat(response.testEndDate).isEqualTo(TestEndDate.of(2020, 4, 23))
        assertThat(response.testKit).isEqualTo(TestKit.LAB_RESULT)
        assertThat(response.diagnosisKeySubmissionSupported).isTrue
        assertThat(response.requiresConfirmatoryTest).isFalse
    }

    @ParameterizedTest
    @EnumSource(value = TestKit::class, names = ["LAB_RESULT", "RAPID_RESULT"])
    fun `app orders from v1, lab uploads on v2, app polls on v1`(testKit: TestKit) {
        val orderResponse = mobileApp.orderTest(V1)

        testLab.uploadTestResult(
            token = orderResponse.tokenParameterValue,
            result = Positive,
            testKit = testKit,
            source = Npex,
            apiVersion = V2
        )

        val pollingToken = orderResponse.testResultPollingToken

        val response = (mobileApp.pollForTestResult(pollingToken, V1) as VirologyLookupResult.Available).response
        assertThat(response.testResult).isEqualTo(Positive)
        assertThat(response.testEndDate).isEqualTo(TestEndDate.of(2020, 4, 23))
        assertThat(response.testKit).isEqualTo(testKit)
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
            assertThat(response).isInstanceOf(VirologyLookupResult.Pending::class.java)
        }

        val responseV2 = mobileApp.pollForTestResult(pollingToken, V2)
        assertThat(responseV2).isInstanceOf(VirologyLookupResult.AvailableV2::class.java)
    }

    @ParameterizedTest
    @EnumSource(value = TestKit::class)
    fun `lab token gen from v1 and app ctaExchange from v2 via different test kits`(testKit: TestKit) {
        val ctaToken = testLab.generateCtaTokenFor(
            testResult = Positive,
            testEndDate = TestEndDate.of(2020, 11, 19),
            source = Eng,
            apiVersion = V1,
            testKit = testKit
        )

        val exchangeResponse = mobileApp.exchange(ctaToken, V2)

        val ctaExchangeResponse = (exchangeResponse as CtaExchangeResult.AvailableV2).ctaExchangeResponse
        assertThat(ctaExchangeResponse.testResult).isEqualTo(Positive)
        assertThat(ctaExchangeResponse.testEndDate).isEqualTo(TestEndDate.of(2020,11,19))
        assertThat(ctaExchangeResponse.testKit).isEqualTo(TestKit.LAB_RESULT)
        assertThat(ctaExchangeResponse.diagnosisKeySubmissionSupported).isTrue
        assertThat(ctaExchangeResponse.requiresConfirmatoryTest).isFalse
    }

    @ParameterizedTest
    @EnumSource(value = TestKit::class, names = ["LAB_RESULT", "RAPID_RESULT"])
    fun `lab token gen from v2 and app ctaExchange from v1`(testKit: TestKit) {
        val ctaToken = testLab.generateCtaTokenFor(
            testResult = Positive,
            testEndDate = TestEndDate.of(2020, 11, 19),
            source = Eng,
            apiVersion = V2,
            testKit = testKit
        )

        val exchangeResponse = mobileApp.exchange(ctaToken, V1)

        val ctaExchangeResponse = (exchangeResponse as CtaExchangeResult.Available).ctaExchangeResponse
        assertThat(ctaExchangeResponse.testResult).isEqualTo(Positive)
        assertThat(ctaExchangeResponse.testEndDate).isEqualTo(TestEndDate.of(2020,11,19))
        assertThat(ctaExchangeResponse.testKit).isEqualTo(testKit)
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
            assertThat(exchangeResponse).isInstanceOf(CtaExchangeResult.NotFound::class.java)
        }

        val exchangeResponseV2 = mobileApp.exchange(ctaToken, V2)
        assertThat(exchangeResponseV2).isInstanceOf(CtaExchangeResult.AvailableV2::class.java)
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
        assertThat(response1).isInstanceOf(VirologyLookupResult.Available::class.java)

        val response2 = mobileApp.pollForTestResult(pollingToken, V2, England)
        assertThat(response2).isInstanceOf(VirologyLookupResult.AvailableV2::class.java)
    }
}
