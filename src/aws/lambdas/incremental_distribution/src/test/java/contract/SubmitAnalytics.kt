package contract

import contract.infra.BackendContractScenario
import contract.infra.RecordingTest
import contract.infra.ReplayTest
import org.junit.jupiter.api.Test
import smoke.actors.MobileApp
import smoke.actors.MobileOS.Android
import smoke.actors.MobileOS.iOS
import smoke.data.AnalyticsMetricsData
import smoke.env.SmokeTests
import uk.nhs.nhsx.analyticssubmission.model.AnalyticsWindow
import uk.nhs.nhsx.core.DateFormatValidator
import java.time.OffsetDateTime

interface SubmitAnalyticsKeys : BackendContractScenario {

    @Test
    fun `Mobile app submits ios analytics data`() {
        control.addNote("Mobile app submits ios analytics data")

        MobileApp(mitmHttpClient(), envConfig, iOS).submitAnalyticsKeys(
            AnalyticsWindow(
                DateFormatValidator.formatter.format(OffsetDateTime.now().minusDays(1)),
                DateFormatValidator.formatter.format(OffsetDateTime.now().plusDays(1))),
            AnalyticsMetricsData.populatedAnalyticsMetrics())
    }

    @Test
    fun `Mobile app submits android analytics data`() {
        control.addNote("Mobile app submits android analytics data")

        MobileApp(mitmHttpClient(), envConfig, Android).submitAnalyticsKeys(
            AnalyticsWindow(
                DateFormatValidator.formatter.format(OffsetDateTime.now().minusDays(1)),
                DateFormatValidator.formatter.format(OffsetDateTime.now().plusDays(1))),
            AnalyticsMetricsData.populatedAnalyticsMetrics())
    }
}

class RecordingSubmitAnalyticsKeysTest : RecordingTest(), SubmitAnalyticsKeys {
    override val envConfig = SmokeTests.loadConfig()
}

class ReplaySubmitAnalyticsKeysTest : ReplayTest(), SubmitAnalyticsKeys {
    override val envConfig = SmokeTests.loadConfig()
}