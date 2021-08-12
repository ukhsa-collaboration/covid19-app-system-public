package smoke

import org.http4k.cloudnative.env.Environment
import org.junit.jupiter.api.Test
import smoke.actors.MobileApp
import smoke.actors.createHandler
import smoke.env.SmokeTests

class ExposureCircuitBreakerSmokeTest {

    private val config = SmokeTests.loadConfig()
    private val client = createHandler(Environment.ENV)

    private val mobileApp = MobileApp(client, config)

    @Test
    fun `mobile app requests circuit break based on exposure`() {
        mobileApp.exposureCircuitBreaker.requestAndApproveCircuitBreak()
    }
}

