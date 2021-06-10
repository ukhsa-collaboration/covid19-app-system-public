package smoke

import org.http4k.client.JavaHttpClient
import org.http4k.filter.debug
import org.junit.jupiter.api.Test
import smoke.actors.MobileApp
import smoke.env.SmokeTests

class ExposureCircuitBreakerSmokeTest {

    private val config = SmokeTests.loadConfig()
    private val client = JavaHttpClient().debug()

    private val mobileApp = MobileApp(client, config)

    @Test
    fun `mobile app requests circuit break based on exposure`() {
        mobileApp.exposureCircuitBreaker.requestAndApproveCircuitBreak()
    }
}

