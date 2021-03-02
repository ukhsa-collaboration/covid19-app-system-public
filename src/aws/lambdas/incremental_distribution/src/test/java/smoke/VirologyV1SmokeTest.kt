package smoke

import org.assertj.core.api.Assertions.assertThat
import org.http4k.client.JavaHttpClient
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import smoke.actors.ApiVersion.V1
import smoke.actors.MobileApp
import smoke.actors.TestLab
import smoke.actors.TestResult.NEGATIVE
import smoke.actors.TestResult.POSITIVE
import smoke.actors.TestResultPollingToken
import smoke.env.SmokeTests
import uk.nhs.nhsx.virology.CtaToken
import uk.nhs.nhsx.virology.TestKit.LAB_RESULT
import uk.nhs.nhsx.virology.VirologyUploadHandler.VirologyResultSource
import uk.nhs.nhsx.virology.VirologyUploadHandler.VirologyResultSource.Npex
import uk.nhs.nhsx.virology.VirologyUploadHandler.VirologyTokenExchangeSource
import uk.nhs.nhsx.virology.exchange.CtaExchangeResult.Available
import uk.nhs.nhsx.virology.exchange.CtaExchangeResult.NotFound
import uk.nhs.nhsx.virology.result.VirologyLookupResult

class VirologyV1SmokeTest {

    private val client = JavaHttpClient()
    private val config = SmokeTests.loadConfig()
    private val mobileApp = MobileApp(client, config)
    private val testLab = TestLab(client, config)

    @ParameterizedTest
    @EnumSource(VirologyResultSource::class)
    fun `order, upload and poll via different sources`(source: VirologyResultSource) {
        val orderResponse = mobileApp.orderTest(V1)

        testLab.uploadTestResult(
            token = CtaToken.of(orderResponse.tokenParameterValue),
            result = POSITIVE,
            source = source,
            apiVersion = V1,
            testKit = LAB_RESULT
        )
        val pollingToken = TestResultPollingToken(orderResponse.testResultPollingToken)
        val testResponse = (mobileApp.pollForTestResult(pollingToken, V1) as VirologyLookupResult.Available).virologyLookupResponse

        assertThat(testResponse.testResult).isEqualTo(POSITIVE.name)
        assertThat(testResponse.testEndDate).isEqualTo("2020-04-23T00:00:00Z")
        assertThat(testResponse.testKit).isEqualTo(LAB_RESULT)
    }

    @ParameterizedTest
    @EnumSource(VirologyTokenExchangeSource::class)
    fun `lab token gen and ctaExchange via different sources`(source: VirologyTokenExchangeSource) {
        val ctaToken = testLab.generateCtaTokenFor(
            testResult = POSITIVE,
            testEndDate = "2020-11-19T00:00:00Z",
            source = source,
            apiVersion = V1,
            testKit = LAB_RESULT
        )

        val exchangeResponse = mobileApp.exchange(ctaToken, V1)

        val ctaExchangeResponse = (exchangeResponse as Available).ctaExchangeResponse
        assertThat(ctaExchangeResponse.testResult).isEqualTo(POSITIVE.name)
        assertThat(ctaExchangeResponse.testEndDate).isEqualTo("2020-11-19T00:00:00Z")
        assertThat(ctaExchangeResponse.testKit).isEqualTo(LAB_RESULT)
    }

    @Test
    fun `test result can be polled more than twice`() {
        val orderResponse = mobileApp.orderTest(V1)
        testLab.uploadTestResult(
            token = CtaToken.of(orderResponse.tokenParameterValue),
            result = POSITIVE,
            source = Npex,
            apiVersion = V1,
            testKit = LAB_RESULT
        )

        repeat(times = 3) {
            val pollingToken = TestResultPollingToken(orderResponse.testResultPollingToken)
            val testResponse = (mobileApp.pollForTestResult(pollingToken, V1) as VirologyLookupResult.Available).virologyLookupResponse
            assertThat(testResponse.testResult).isEqualTo(POSITIVE.name)
        }
    }

    @Test
    fun `not found when exchanging cta token more than twice`() {
        val ctaToken = testLab.generateCtaTokenFor(
            testResult = NEGATIVE,
            testEndDate = "2020-11-17T00:00:00Z",
            source = VirologyTokenExchangeSource.Eng,
            apiVersion = V1,
            testKit = LAB_RESULT
        )

        val firstCall = mobileApp.exchange(ctaToken, V1)
        assertThat(firstCall).isInstanceOf(Available::class.java)

        val secondCall = mobileApp.exchange(ctaToken, V1)
        assertThat(secondCall).isInstanceOf(Available::class.java)

        val thirdCall = mobileApp.exchange(ctaToken, V1)
        assertThat(thirdCall).isInstanceOf(NotFound::class.java)
    }

}
