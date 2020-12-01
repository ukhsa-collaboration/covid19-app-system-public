package smoke

import org.assertj.core.api.Assertions.assertThat
import org.http4k.client.JavaHttpClient
import org.junit.jupiter.api.Test
import smoke.clients.DiagnosisKeysSubmissionClient
import smoke.clients.VirologyClient
import smoke.env.SmokeTests
import uk.nhs.nhsx.core.random.crockford.CrockfordDammRandomStringGenerator
import uk.nhs.nhsx.virology.VirologyUploadHandler.VirologyTokenExchangeSource
import uk.nhs.nhsx.virology.exchange.CtaExchangeResult.Available
import uk.nhs.nhsx.virology.exchange.CtaExchangeResult.NotFound
import java.util.*
import kotlin.random.Random

class VirologySmokeTest {

    private val client = JavaHttpClient()
    private val config = SmokeTests.loadConfig()
    private val virologyClient = VirologyClient(client, config)
    private val diagnosisKeysSubmissionClient = DiagnosisKeysSubmissionClient(client, config)
    private val keyGenerator = CrockfordDammRandomStringGenerator()

    @Test
    fun `eng token gen`() {
        val testLabResponse = virologyClient.ctaTokenGen(
            testResult = "POSITIVE",
            testEndDate = "2020-11-19T00:00:00Z",
            source = VirologyTokenExchangeSource.Eng
        )
        val exchangeResponse = virologyClient.exchangeCtaToken(testLabResponse)
        assertThat(exchangeResponse).isInstanceOf(Available::class.java)

        val ctaExchangeResponse = (exchangeResponse as Available).ctaExchangeResponse
        assertThat(ctaExchangeResponse.testResult).isEqualTo("POSITIVE")
        assertThat(ctaExchangeResponse.testEndDate).isEqualTo("2020-11-19T00:00:00Z")
    }

    @Test
    fun `wls token gen`() {
        val testLabResponse = virologyClient.ctaTokenGen(
            testResult = "NEGATIVE",
            testEndDate = "2020-11-18T00:00:00Z",
            source = VirologyTokenExchangeSource.Wls
        )
        val exchangeResponse = virologyClient.exchangeCtaToken(testLabResponse)
        assertThat(exchangeResponse).isInstanceOf(Available::class.java)

        val ctaExchangeResponse = (exchangeResponse as Available).ctaExchangeResponse
        assertThat(ctaExchangeResponse.testResult).isEqualTo("NEGATIVE")
        assertThat(ctaExchangeResponse.testEndDate).isEqualTo("2020-11-18T00:00:00Z")
    }

    @Test
    fun `self administered token gen`() {
        val testLabResponse = virologyClient.ctaTokenGen(
            testResult = "VOID",
            testEndDate = "2020-11-17T00:00:00Z",
            source = VirologyTokenExchangeSource.Lfd
        )
        val exchangeResponse = virologyClient.exchangeCtaToken(testLabResponse)
        assertThat(exchangeResponse).isInstanceOf(Available::class.java)

        val ctaExchangeResponse = (exchangeResponse as Available).ctaExchangeResponse
        assertThat(ctaExchangeResponse.testResult).isEqualTo("VOID")
        assertThat(ctaExchangeResponse.testEndDate).isEqualTo("2020-11-17T00:00:00Z")
    }

    @Test
    fun `not found when exchanging cta token more than twice`() {
        val testLabResponse = virologyClient.ctaTokenGen("NEGATIVE")

        val firstCall = virologyClient.exchangeCtaToken(testLabResponse)
        assertThat(firstCall).isInstanceOf(Available::class.java)

        val secondCall = virologyClient.exchangeCtaToken(testLabResponse)
        assertThat(secondCall).isInstanceOf(Available::class.java)

        val thirdCall = virologyClient.exchangeCtaToken(testLabResponse)
        assertThat(thirdCall).isInstanceOf(NotFound::class.java)
    }

    @Test
    fun `result for virology test ordered via api can be polled more than twice`() {
        val orderResponse = virologyClient.orderTest()
        virologyClient.uploadTestResult(orderResponse, "POSITIVE")

        repeat(times = 3) {
            val lookupResponse = virologyClient.retrieveTestResult(orderResponse.testResultPollingToken)
            assertThat(lookupResponse.testResult).isEqualTo("POSITIVE")
        }
    }

    @Test
    fun `exchange token exchange keys and exchange token again without db exception`() {
        val testLabResponse = virologyClient.ctaTokenGen("POSITIVE")

        val firstCall = virologyClient.exchangeCtaToken(testLabResponse)
        assertThat(firstCall).isInstanceOf(Available::class.java)

        val diagnosisKeysSubmissionToken = (firstCall as Available).ctaExchangeResponse.diagnosisKeySubmissionToken
        submitKeys(diagnosisKeysSubmissionToken)

        val secondCall = virologyClient.exchangeCtaToken(testLabResponse)
        assertThat(secondCall).isInstanceOf(Available::class.java)

        val thirdCall = virologyClient.exchangeCtaToken(testLabResponse)
        assertThat(thirdCall).isInstanceOf(NotFound::class.java)
    }

    fun submitKeys(diagnosisKeySubmissionToken: String,
                   encodedSubmissionKeys: List<String> = generateKeyData(Random.nextInt(10))): List<String> {
        val payload = diagnosisKeysSubmissionClient.createKeysPayload(
            diagnosisKeySubmissionToken,
            encodedSubmissionKeys
        )
        diagnosisKeysSubmissionClient.sendTempExposureKeys(payload)
        return encodedSubmissionKeys
    }

    private fun generateKeyData(numKeys: Int) =
        (0..numKeys)
            .map { keyGenerator.generate() + keyGenerator.generate() }
            .map { Base64.getEncoder().encodeToString(it.toByteArray()) }
}