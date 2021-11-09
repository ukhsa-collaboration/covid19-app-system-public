package smoke

import assertions.CtaExchangeAssertionsV1.testEndDate
import assertions.CtaExchangeAssertionsV1.testKit
import assertions.CtaExchangeAssertionsV1.testResult
import assertions.VirologyAssertionsV1.testEndDate
import assertions.VirologyAssertionsV1.testKit
import assertions.VirologyAssertionsV1.testResult
import org.http4k.cloudnative.env.Environment
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import smoke.actors.ApiVersion.V1
import smoke.actors.MobileApp
import smoke.actors.TestLab
import smoke.actors.createHandler
import smoke.env.SmokeTests
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import uk.nhs.nhsx.domain.TestEndDate
import uk.nhs.nhsx.domain.TestKit.LAB_RESULT
import uk.nhs.nhsx.domain.TestResult.Negative
import uk.nhs.nhsx.domain.TestResult.Positive
import uk.nhs.nhsx.virology.VirologyUploadHandler.VirologyResultSource
import uk.nhs.nhsx.virology.VirologyUploadHandler.VirologyResultSource.Npex
import uk.nhs.nhsx.virology.VirologyUploadHandler.VirologyTokenExchangeSource
import uk.nhs.nhsx.virology.exchange.CtaExchangeResult.AvailableV1
import uk.nhs.nhsx.virology.exchange.CtaExchangeResult.NotFound
import uk.nhs.nhsx.virology.lookup.VirologyLookupResult

class VirologyV1SmokeTest {

    private val client = createHandler(Environment.ENV)
    private val config = SmokeTests.loadConfig()
    private val mobileApp = MobileApp(client, config)
    private val testLab = TestLab(client, config)

    @ParameterizedTest
    @EnumSource(VirologyResultSource::class)
    fun `order, upload and poll via different sources`(source: VirologyResultSource) {
        val orderResponse = mobileApp.orderTest(V1)

        testLab.uploadTestResult(
            token = orderResponse.tokenParameterValue,
            result = Positive,
            source = source,
            apiVersion = V1,
            testKit = LAB_RESULT
        )
        val pollingToken = orderResponse.testResultPollingToken
        val testResponse = mobileApp.pollForTestResult(pollingToken, V1)

        expectThat(testResponse)
            .isA<VirologyLookupResult.AvailableV1>()
            .get(VirologyLookupResult.AvailableV1::response).and {
                testResult.isEqualTo(Positive)
                testEndDate.isEqualTo(TestEndDate.of(2020, 4, 23))
                testKit.isEqualTo(LAB_RESULT)
            }
    }

    @ParameterizedTest
    @EnumSource(VirologyTokenExchangeSource::class)
    fun `lab token gen and ctaExchange via different sources`(source: VirologyTokenExchangeSource) {
        val ctaToken = testLab.generateCtaTokenFor(
            testResult = Positive,
            testEndDate = TestEndDate.of(2020, 11, 19),
            source = source,
            apiVersion = V1,
            testKit = LAB_RESULT
        )

        val exchangeResponse = mobileApp.exchange(ctaToken, V1)

        expectThat(exchangeResponse)
            .isA<AvailableV1>()
            .get(AvailableV1::ctaExchangeResponse).and {
                testResult.isEqualTo(Positive)
                testEndDate.isEqualTo(TestEndDate.of(2020, 11, 19))
                testKit.isEqualTo(LAB_RESULT)
            }
    }

    @Test
    fun `test result can be polled more than twice`() {
        val orderResponse = mobileApp.orderTest(V1)
        testLab.uploadTestResult(
            token = orderResponse.tokenParameterValue,
            result = Positive,
            source = Npex,
            apiVersion = V1,
            testKit = LAB_RESULT
        )

        repeat(times = 3) {
            val pollingToken = orderResponse.testResultPollingToken
            val testResponse = mobileApp.pollForTestResult(pollingToken, V1)

            expectThat(testResponse)
                .isA<VirologyLookupResult.AvailableV1>()
                .get(VirologyLookupResult.AvailableV1::response)
                .testResult
                .isEqualTo(Positive)
        }
    }

    @Test
    fun `not found when exchanging cta token more than twice`() {
        val ctaToken = testLab.generateCtaTokenFor(
            testResult = Negative,
            testEndDate = TestEndDate.of(2020, 11, 19),
            source = VirologyTokenExchangeSource.Eng,
            apiVersion = V1,
            testKit = LAB_RESULT
        )

        val firstCall = mobileApp.exchange(ctaToken, V1)
        expectThat(firstCall).isA<AvailableV1>()

        val secondCall = mobileApp.exchange(ctaToken, V1)
        expectThat(secondCall).isA<AvailableV1>()

        val thirdCall = mobileApp.exchange(ctaToken, V1)
        expectThat(thirdCall).isA<NotFound>()
    }
}
