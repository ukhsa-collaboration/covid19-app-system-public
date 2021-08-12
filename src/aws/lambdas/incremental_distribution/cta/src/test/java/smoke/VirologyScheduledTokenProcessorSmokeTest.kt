package smoke

import assertions.AwsSdkAssertions.asString
import org.junit.jupiter.api.Test
import smoke.actors.BackgroundActivities
import smoke.env.SmokeTests
import software.amazon.awssdk.services.lambda.model.InvokeResponse
import strikt.api.expectThat
import strikt.assertions.contains

class VirologyScheduledTokenProcessorSmokeTest {

    private val config = SmokeTests.loadConfig()
    private val backgroundActivities = BackgroundActivities(config)

    @Test
    fun `run virology scheduled token processor should return success`() {
        val result: InvokeResponse = backgroundActivities.invokeScheduledVirologyTokenProcessor()

        expectThat(result)
            .get(InvokeResponse::payload)
            .asString()
            .contains("CtaTokensAndUrlGenerationCompleted")
    }
}
