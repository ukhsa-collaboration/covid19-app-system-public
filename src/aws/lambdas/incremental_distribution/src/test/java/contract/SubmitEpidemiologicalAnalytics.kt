package contract

import contract.infra.BackendContractScenario
import contract.infra.RecordingTest
import contract.infra.ReplayTest
import org.junit.jupiter.api.Test
import smoke.actors.MobileApp
import smoke.actors.MobileOS
import smoke.data.AnalyticsEventsData
import smoke.env.SmokeTests
import java.util.UUID

interface SubmitEpidemiologicalAnalytics : BackendContractScenario {

    @Test
    fun `Mobile app submits analytics events`() {
        control.addNote("Mobile app submits analytics events")
        MobileApp(mitmHttpClient(), envConfig, MobileOS.iOS).submitAnalyticEvents(AnalyticsEventsData.analyticsEvents(UUID(1, 1)))
    }
}

class RecordingSubmitEpidemiologicalAnalyticsTest : RecordingTest(), SubmitEpidemiologicalAnalytics {
    override val envConfig = SmokeTests.loadConfig()
}

class ReplaySubmitEpidemiologicalAnalyticsTest : ReplayTest(), SubmitEpidemiologicalAnalytics {
    override val envConfig = SmokeTests.loadConfig()
}