package smoke

import com.amazonaws.services.lambda.AWSLambda
import org.assertj.core.api.Assertions.assertThat
import org.http4k.client.JavaHttpClient
import org.junit.Test
import smoke.clients.AwsLambda
import smoke.clients.VirologyClient
import smoke.env.SmokeTests
import uk.nhs.nhsx.virology.exchange.CtaExchangeResult

class VirologySmokeTest {
    private val client = JavaHttpClient()
    private val config = SmokeTests.loadConfig()
    private val virologyClient = VirologyClient(client, config)

    @Test
    fun `not found when exchanging cta token more than twice`() {
        val testLabResponse = virologyClient.ctaTokenGen("NEGATIVE")

        val exchangeResponseFirstCall: CtaExchangeResult = virologyClient.exchangeCtaToken(testLabResponse)
        val exchangeResponseSecondCall: CtaExchangeResult = virologyClient.exchangeCtaToken(testLabResponse)
        val exchangeResponseThirdCall = virologyClient.exchangeCtaToken(testLabResponse)
        assertThat(exchangeResponseFirstCall).isInstanceOf(CtaExchangeResult.Available::class.java)
        assertThat(exchangeResponseSecondCall).isInstanceOf(CtaExchangeResult.Available::class.java)
        assertThat(exchangeResponseThirdCall).isInstanceOf(CtaExchangeResult.NotFound::class.java)
    }

    @Test
    fun `result for virology test ordered via api can be polled more than twice`() {
        val virologyOrderResponse = virologyClient.orderTest()
        virologyClient.uploadTestResult(virologyOrderResponse, "POSITIVE")

        repeat(times = 3) {
            val virologyLookupResponse = virologyClient.retrieveTestResult(virologyOrderResponse.testResultPollingToken)
            assertThat(virologyLookupResponse.testResult).isEqualTo("POSITIVE")
        }

    }
}