package contract

import contract.infra.BackendContractScenario
import contract.infra.RecordingTest
import contract.infra.ReplayTest
import org.junit.jupiter.api.Test
import smoke.actors.IpcToken
import smoke.actors.MobileApp
import smoke.actors.UserCountry
import smoke.env.SmokeTests
import java.time.OffsetDateTime

interface CreateAndUpdateIsolationToken : BackendContractScenario {
    @Test
    @JvmDefault
    fun `Mobile user creates and submits isolation token`() {
        val mobileApp = MobileApp(mitmHttpClient(), envConfig)

        control.addNote("Mobile App creates isolation token")
        val ipcToken = IpcToken(mobileApp.createIsolationToken(UserCountry.England).ipcToken)

        control.addNote("Mobile App updates isolation token with new dates")
        mobileApp.updateIsolationToken(ipcToken, OffsetDateTime.now().minusDays(4), OffsetDateTime.now().minusDays(4))
    }
}

class RecordingCreateAndUpdateIsolationTokenTest : RecordingTest(), CreateAndUpdateIsolationToken {
    override val envConfig = SmokeTests.loadConfig()
}

class ReplayCreateAndUpdateIsolationTokenTest : ReplayTest(), CreateAndUpdateIsolationToken {
    override val envConfig = SmokeTests.loadConfig()
}