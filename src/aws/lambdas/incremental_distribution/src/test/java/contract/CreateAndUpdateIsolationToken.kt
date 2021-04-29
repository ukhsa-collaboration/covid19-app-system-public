package contract

import contract.infra.BackendContractScenario
import contract.infra.RecordingTest
import contract.infra.ReplayTest
import org.junit.jupiter.api.Test
import smoke.actors.MobileApp
import smoke.env.SmokeTests
import uk.nhs.nhsx.domain.Country.Companion.England
import java.time.Duration
import java.time.Instant

interface CreateAndUpdateIsolationToken : BackendContractScenario {
    @Test
    @JvmDefault
    fun `Mobile user creates and submits isolation token`() {
        val mobileApp = MobileApp(mitmHttpClient(), envConfig)

        control.addNote("Mobile App creates isolation token")

        val x = mobileApp.createIsolationToken(England)
        control.addNote("Mobile App updates isolation token with new dates")
        mobileApp.updateIsolationToken(
            x.ipcToken,
            Instant.now().minus(Duration.ofDays(4)),
            Instant.now().minus(Duration.ofDays(4))
        )
    }
}

class RecordingCreateAndUpdateIsolationTokenTest : RecordingTest(), CreateAndUpdateIsolationToken {
    override val envConfig = SmokeTests.loadConfig()
}

class ReplayCreateAndUpdateIsolationTokenTest : ReplayTest(), CreateAndUpdateIsolationToken {
    override val envConfig = SmokeTests.loadConfig()
}
