package smoke

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import smoke.actors.BackgroundActivities
import smoke.env.SmokeTests

class VirologyScheduledTokenProcessorSmokeTest {

    private val config = SmokeTests.loadConfig()
    private val backgroundActivities = BackgroundActivities(config)

    @Test
    fun `run virology scheduled token processor should return success`() {
        val result = backgroundActivities.invokeScheduledVirologyTokenProcessor()
        Assertions.assertThat(result.payload().asUtf8String()).contains("CtaTokensAndUrlGenerationCompleted")
    }

}
