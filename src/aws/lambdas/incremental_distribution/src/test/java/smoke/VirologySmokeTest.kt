package smoke

import org.assertj.core.api.Assertions.assertThat
import org.http4k.client.JavaHttpClient
import org.junit.jupiter.api.Test
import smoke.actors.MobileApp
import smoke.actors.TestLab
import smoke.actors.ApiVersion.V1
import smoke.actors.ApiVersion.V2
import smoke.actors.DiagnosisKeySubmissionToken
import smoke.actors.TestResult.NEGATIVE
import smoke.actors.TestResult.POSITIVE
import smoke.actors.TestResult.VOID
import smoke.env.SmokeTests
import uk.nhs.nhsx.core.random.crockford.CrockfordDammRandomStringGenerator
import uk.nhs.nhsx.virology.CtaToken
import uk.nhs.nhsx.virology.TestKit
import uk.nhs.nhsx.virology.TestKit.LAB_RESULT
import uk.nhs.nhsx.virology.TestKit.RAPID_RESULT
import uk.nhs.nhsx.virology.VirologyUploadHandler.VirologyResultSource.Fiorano
import uk.nhs.nhsx.virology.VirologyUploadHandler.VirologyResultSource.Npex
import uk.nhs.nhsx.virology.VirologyUploadHandler.VirologyTokenExchangeSource
import uk.nhs.nhsx.virology.exchange.CtaExchangeResult.Available
import uk.nhs.nhsx.virology.exchange.CtaExchangeResult.NotFound
import java.util.*
import kotlin.random.Random

class VirologySmokeTest {

    private val client = JavaHttpClient()
    private val config = SmokeTests.loadConfig()
    private val mobileApp = MobileApp(client, config)
    private val testLab = TestLab(client, config)
    private val keyGenerator = CrockfordDammRandomStringGenerator()

    @Test
    fun `eng token gen v1`() {
        val ctaToken = testLab.generateCtaTokenFor(
            testResult = POSITIVE,
            testEndDate = "2020-11-19T00:00:00Z",
            source = VirologyTokenExchangeSource.Eng,
            apiVersion = V1,
            testKit = LAB_RESULT
        )
        val exchangeResponse = mobileApp.exchange(ctaToken, V1)
        assertThat(exchangeResponse).isInstanceOf(Available::class.java)

        val ctaExchangeResponse = (exchangeResponse as Available).ctaExchangeResponse
        assertThat(ctaExchangeResponse.testResult).isEqualTo(POSITIVE.name)
        assertThat(ctaExchangeResponse.testEndDate).isEqualTo("2020-11-19T00:00:00Z")
        assertThat(ctaExchangeResponse.testKit).isEqualTo(LAB_RESULT)
    }

    @Test
    fun `wls token gen v1`() {
        val ctaToken = testLab.generateCtaTokenFor(
            testResult = NEGATIVE,
            testEndDate = "2020-11-18T00:00:00Z",
            source = VirologyTokenExchangeSource.Wls,
            apiVersion = V1,
            testKit = LAB_RESULT
        )

        val exchangeResponse = mobileApp.exchange(ctaToken, V1)
        assertThat(exchangeResponse).isInstanceOf(Available::class.java)

        val ctaExchangeResponse = (exchangeResponse as Available).ctaExchangeResponse
        assertThat(ctaExchangeResponse.testResult).isEqualTo(NEGATIVE.name)
        assertThat(ctaExchangeResponse.testEndDate).isEqualTo("2020-11-18T00:00:00Z")
        assertThat(ctaExchangeResponse.testKit).isEqualTo(LAB_RESULT)
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

    @Test
    fun `result for virology test ordered via api can be polled more than twice`() {
        val orderResponse = mobileApp.orderTest(V1)
        testLab.uploadTestResult(
            token = CtaToken.of(orderResponse.tokenParameterValue),
            result = POSITIVE,
            source = Npex,
            apiVersion = V1,
            testKit = LAB_RESULT
        )

        repeat(times = 3) {
            assertThat(mobileApp.pollForCompleteTestResult(V1).testResult).isEqualTo(POSITIVE.name)
        }
    }

    @Test
    fun `exchange token exchange keys and exchange token again without db exception`() {
        val ctaToken = testLab.generateCtaTokenFor(
            testResult = POSITIVE,
            testEndDate = "2020-11-17T00:00:00Z",
            source = VirologyTokenExchangeSource.Eng,
            apiVersion = V1,
            testKit = LAB_RESULT
        )

        val firstCall = mobileApp.exchange(ctaToken, V1)
        assertThat(firstCall).isInstanceOf(Available::class.java)

        val diagnosisKeysSubmissionToken = (firstCall as Available).ctaExchangeResponse.diagnosisKeySubmissionToken
        mobileApp.submitKeys(
            DiagnosisKeySubmissionToken(diagnosisKeysSubmissionToken),
            generateKeyData(Random.nextInt(10))
        )

        val secondCall = mobileApp.exchange(ctaToken, V1)
        assertThat(secondCall).isInstanceOf(Available::class.java)

        val thirdCall = mobileApp.exchange(ctaToken, V1)
        assertThat(thirdCall).isInstanceOf(NotFound::class.java)
    }

    @Test
    fun `test lab result uploaded via npex v1 api is success`() {
        val orderResponse = mobileApp.orderTest(V1)

        testLab.uploadTestResult(
            token = CtaToken.of(orderResponse.tokenParameterValue),
            result = POSITIVE,
            source = Npex,
            apiVersion = V1,
            testKit = LAB_RESULT
        )

        val testResponse = mobileApp.pollForCompleteTestResult(V1)

        assertThat(testResponse.testResult).isEqualTo(POSITIVE.name)
        assertThat(testResponse.testKit).isEqualTo(LAB_RESULT)
    }

    @Test
    fun `test lab result uploaded via npex v2 api is success`() {
        val orderResponse = mobileApp.orderTest(V2)

        testLab.uploadTestResult(
            token = CtaToken.of(orderResponse.tokenParameterValue),
            result = POSITIVE,
            testKit = RAPID_RESULT,
            source = Npex,
            apiVersion = V2
        )

        val testResponse = mobileApp.pollForCompleteTestResult(V2)

        assertThat(testResponse.testResult).isEqualTo(POSITIVE.name)
        assertThat(testResponse.testKit).isEqualTo(RAPID_RESULT)
    }

    @Test
    fun `test lab result upload via npex v2 api does not accept lfd negative test`() {
        val orderResponse = mobileApp.orderTest(V2)

        testLab.uploadTestResultWithUnprocessableEntityV2(
            token = CtaToken.of(orderResponse.tokenParameterValue),
            result = NEGATIVE,
            testKit = RAPID_RESULT,
            source = Npex
        )
    }

    @Test
    fun `test lab result uploaded via fiorano v1 api is success`() {
        val orderResponse = mobileApp.orderTest(V1)

        testLab.uploadTestResult(
            token = CtaToken.of(orderResponse.tokenParameterValue),
            result = POSITIVE,
            source = Fiorano,
            apiVersion = V1,
            testKit = LAB_RESULT
        )

        val testResponse = mobileApp.pollForCompleteTestResult(V1)

        assertThat(testResponse.testResult).isEqualTo(POSITIVE.name)
        assertThat(testResponse.testKit).isEqualTo(LAB_RESULT)
    }

    @Test
    fun `test lab result uploaded via fiorano v2 api is success`() {
        val orderResponse = mobileApp.orderTest(V2)

        testLab.uploadTestResult(
            token = CtaToken.of(orderResponse.tokenParameterValue),
            result = POSITIVE,
            testKit = RAPID_RESULT,
            source = Fiorano,
            apiVersion = V2
        )

        val testResponse = mobileApp.pollForCompleteTestResult(V2)

        assertThat(testResponse.testResult).isEqualTo(POSITIVE.name)
        assertThat(testResponse.testKit).isEqualTo(RAPID_RESULT)
    }

    @Test
    fun `eng token gen v2`() {
        val ctaToken = testLab.generateCtaTokenFor(
            testResult = POSITIVE,
            testEndDate = "2020-11-19T00:00:00Z",
            source = VirologyTokenExchangeSource.Eng,
            apiVersion = V2, testKit = RAPID_RESULT)
        val exchangeResponse = mobileApp.exchange(ctaToken, V2)
        assertThat(exchangeResponse).isInstanceOf(Available::class.java)

        val ctaExchangeResponse = (exchangeResponse as Available).ctaExchangeResponse
        assertThat(ctaExchangeResponse.testResult).isEqualTo(POSITIVE.name)
        assertThat(ctaExchangeResponse.testEndDate).isEqualTo("2020-11-19T00:00:00Z")
        assertThat(ctaExchangeResponse.testKit).isEqualTo(RAPID_RESULT)
    }

    @Test
    fun `wls token gen v2`() {
        val ctaToken = testLab.generateCtaTokenFor(
            testResult = NEGATIVE,
            testEndDate = "2020-11-19T00:00:00Z",
            source = VirologyTokenExchangeSource.Wls,
            apiVersion = V2, testKit = TestKit.LAB_RESULT)
        val exchangeResponse = mobileApp.exchange(ctaToken, V2)
        assertThat(exchangeResponse).isInstanceOf(Available::class.java)

        val ctaExchangeResponse = (exchangeResponse as Available).ctaExchangeResponse
        assertThat(ctaExchangeResponse.testResult).isEqualTo(NEGATIVE.name)
        assertThat(ctaExchangeResponse.testEndDate).isEqualTo("2020-11-19T00:00:00Z")
        assertThat(ctaExchangeResponse.testKit).isEqualTo(LAB_RESULT)
    }

    @Test
    fun `polling token for LFD test result found in both v1 and v2`() {
        val orderResponse = mobileApp.orderTest(V2)

        testLab.uploadTestResult(
            token = CtaToken.of(orderResponse.tokenParameterValue),
            result = POSITIVE,
            testKit = RAPID_RESULT,
            source = Npex,
            apiVersion = V2
        )

        mobileApp.pollForCompleteTestResult(V1)
        mobileApp.pollForCompleteTestResult(V2)
    }

    private fun generateKeyData(numKeys: Int) =
        (0..numKeys)
            .map { keyGenerator.generate() + keyGenerator.generate() }
            .map { Base64.getEncoder().encodeToString(it.toByteArray()) }
}
