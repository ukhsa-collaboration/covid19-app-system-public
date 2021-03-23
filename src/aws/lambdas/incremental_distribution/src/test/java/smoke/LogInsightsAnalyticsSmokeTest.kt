package smoke

import com.natpryce.hamkrest.containsSubstring
import org.junit.jupiter.api.Test
import smoke.actors.BackgroundActivities
import smoke.actors.requireBodyText
import smoke.env.SmokeTests
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

class LogInsightsAnalyticsSmokeTest {
    private val config = SmokeTests.loadConfig()
    private val backgroundActivities = BackgroundActivities(config)

    @Test
    fun `invoking lambda returns success within time window`() {
        val instantSetTo1am = Instant.from(ZonedDateTime.of(2021, 1, 2, 1, 0, 0, 0, ZoneOffset.UTC))
        listOf(
            config.exposure_notification_circuit_breaker_analytics_lambda_function_name,
            config.federation_keys_download_analytics_lambda_function_name,
            config.federation_keys_upload_analytics_lambda_function_name
        ).forEach { function ->
            backgroundActivities.invokeAnalyticsLogs(scheduledEventTime = instantSetTo1am, functionName = function)
                .requireBodyText(containsSubstring("AnalyticsUploadedToS3"))
        }
    }

    @Test
    fun `invoking lambda returns error when outside of time window`() {
        val instantSetTo6am = Instant.from(ZonedDateTime.of(2021, 1, 2, 6, 0, 0, 0, ZoneOffset.UTC))
        listOf(
            config.exposure_notification_circuit_breaker_analytics_lambda_function_name,
            config.federation_keys_download_analytics_lambda_function_name,
            config.federation_keys_upload_analytics_lambda_function_name
        ).forEach { function ->
            backgroundActivities.invokeAnalyticsLogs(scheduledEventTime = instantSetTo6am, functionName = function)
                .requireBodyText(containsSubstring("CloudWatch Event triggered Lambda at wrong time"))
        }
    }
}
