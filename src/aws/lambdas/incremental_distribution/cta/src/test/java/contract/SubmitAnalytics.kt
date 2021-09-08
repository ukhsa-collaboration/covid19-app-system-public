package contract

import contract.infra.BackendContractScenario
import contract.infra.RecordingTest
import contract.infra.ReplayTest
import org.junit.jupiter.api.Test
import smoke.actors.MobileApp
import smoke.env.SmokeTests
import uk.nhs.nhsx.core.headers.MobileOS.Android
import uk.nhs.nhsx.core.headers.MobileOS.iOS
import java.time.Duration
import java.time.Instant

interface SubmitAnalyticsKeys : BackendContractScenario {

    @Test
    fun `Mobile app submits ios analytics data`() {
        control.addNote("Mobile app submits ios analytics data")

        MobileApp(mitmHttpClient(), envConfig, iOS)
            .submitAnalyticsKeys(
                startDate = Instant.now().minus(Duration.ofDays(1)),
                endDate = Instant.now().plus(Duration.ofDays(1))
            )
    }

    @Test
    fun `Mobile app submits android analytics data`() {
        control.addNote("Mobile app submits android analytics data")

        MobileApp(mitmHttpClient(), envConfig, Android)
            .submitAnalyticsKeys(
                startDate = Instant.now().minus(Duration.ofDays(1)),
                endDate = Instant.now().plus(Duration.ofDays(1))
            )
    }
}

class RecordingSubmitAnalyticsKeysTest : RecordingTest(), SubmitAnalyticsKeys {
    override val envConfig = SmokeTests.loadConfig()
}

class ReplaySubmitAnalyticsKeysTest : ReplayTest(), SubmitAnalyticsKeys {
    override val envConfig = SmokeTests.loadConfig()
}
