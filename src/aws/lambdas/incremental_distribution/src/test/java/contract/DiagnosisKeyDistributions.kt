package contract

import contract.infra.BackendContractScenario
import contract.infra.RecordingTest
import contract.infra.ReplayTest
import org.junit.jupiter.api.Test
import smoke.actors.MobileApp
import smoke.env.SmokeTests
import java.time.LocalDate

interface DiagnosisKeyDistributions : BackendContractScenario {
    @Test
    @JvmDefault
    fun `Mobile app polls for two hourly exposure keys`() {
        MobileApp(mitmHttpClient(), envConfig).getLatestTwoHourlyTekExport()
    }

    @Test
    @JvmDefault
    fun `Mobile app polls for daily exposure keys`() {
        MobileApp(mitmHttpClient(), envConfig).getDailyTekExport(LocalDate.now())
    }
}

class RecordingDiagnosisKeyDistributionsTest : RecordingTest(), DiagnosisKeyDistributions {
    override val envConfig = SmokeTests.loadConfig()
}

class ReplayDiagnosisKeyDistributionsTest : ReplayTest(), DiagnosisKeyDistributions {
    override val envConfig = SmokeTests.loadConfig()
}