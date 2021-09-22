package contract

import contract.infra.BackendContractScenario
import contract.infra.RecordingTest
import contract.infra.ReplayTest
import org.junit.jupiter.api.Test
import smoke.actors.MobileApp
import smoke.env.SmokeTests

interface CircuitBreakers : BackendContractScenario {
    @Test
    fun `Mobile user checks exposure circuit breaker`() {
        MobileApp(mitmHttpClient(), envConfig).exposureCircuitBreaker.requestAndApproveCircuitBreak()
    }

    @Test
    fun `Mobile user checks venue circuit breaker`() {
        MobileApp(mitmHttpClient(), envConfig).venueCircuitBreaker.requestAndApproveCircuitBreak()
    }
}

class RecordingCircuitBreakersTest : RecordingTest(), CircuitBreakers {
    override val envConfig = SmokeTests.loadConfig()
}

class ReplayCircuitBreakersTest : ReplayTest(), CircuitBreakers {
    override val envConfig = SmokeTests.loadConfig()
}
