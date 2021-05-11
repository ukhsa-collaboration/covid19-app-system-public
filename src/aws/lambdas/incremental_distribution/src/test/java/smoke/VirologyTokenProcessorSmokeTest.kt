package smoke

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import smoke.actors.BackgroundActivities
import smoke.env.SmokeTests
import uk.nhs.nhsx.domain.TestEndDate
import uk.nhs.nhsx.domain.TestKit
import uk.nhs.nhsx.domain.TestResult
import uk.nhs.nhsx.virology.tokengen.CtaProcessorRequest
import java.time.LocalDate

class VirologyTokenProcessorSmokeTest {

    private val config = SmokeTests.loadConfig()
    private val backgroundActivities = BackgroundActivities(config)

    @Test
    fun `run virology token processor should return success`() {
        val request = CtaProcessorRequest(
            testResult = TestResult.Positive,
            testEndDate = TestEndDate.of(LocalDate.now()),
            testKit = TestKit.LAB_RESULT,
            numberOfTokens = 1
        )
        val result = backgroundActivities.invokeVirologyTokenProcessor(request)
        assertThat(result.payload().asUtf8String()).contains("success")
    }
}
