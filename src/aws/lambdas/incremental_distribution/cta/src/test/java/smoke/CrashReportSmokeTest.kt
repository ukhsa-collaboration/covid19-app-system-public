package smoke

import org.http4k.client.JavaHttpClient
import org.junit.jupiter.api.Test
import smoke.actors.MobileApp
import smoke.env.SmokeTests
import uk.nhs.nhsx.crashreports.CrashReportRequest

class CrashReportSmokeTest {

    private val client = JavaHttpClient()
    private val config = SmokeTests.loadConfig()
    private val mobileApp = MobileApp(client, config)

    @Test
    fun `submits crash report`() {
        mobileApp.submitCrashReport(
            CrashReportRequest(
                exception = "android.app.RemoteServiceException",
                threadName = "MainThread",
                stackTrace = "android.app.RemoteServiceException: Here is the elusive exception message that we really need to capture"
            )
        )
    }
}
