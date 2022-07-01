package smoke

import org.junit.jupiter.api.Test
import smoke.actors.BackgroundActivities
import smoke.actors.requireBodyContains
import smoke.env.SmokeTests
import uk.nhs.nhsx.analyticslogs.AnalyticsLogsFinished
import java.time.LocalDate
import java.time.LocalTime
import java.time.LocalDateTime
import java.time.ZoneOffset

class LogInsightsAnalyticsSmokeTest {
    private val config = SmokeTests.loadConfig()
    private val backgroundActivities = BackgroundActivities(config)

    @Test
    fun `invoking lambda returns success within time window`() {
        val instantSetTo1am = LocalDateTime.of(LocalDate.now(), LocalTime.of(1, 0, 0)).toInstant(ZoneOffset.UTC)
        listOf(
            config.exposure_notification_circuit_breaker_analytics_lambda_function_name,
            config.federation_keys_download_analytics_lambda_function_name,
            config.federation_keys_upload_analytics_lambda_function_name
        ).forEach {
            backgroundActivities
                .invokeAnalyticsLogs(
                    scheduledEventTime = instantSetTo1am,
                    functionName = it
                )
                .requireBodyContains(AnalyticsLogsFinished::class.java.simpleName)
        }
    }

    @Test
    fun `invoking lambda returns error when outside of time window`() {
        val instantSetTo6am = LocalDateTime.of(LocalDate.now(), LocalTime.of(6, 0, 0)).toInstant(ZoneOffset.UTC)
        listOf(
            config.exposure_notification_circuit_breaker_analytics_lambda_function_name,
            config.federation_keys_download_analytics_lambda_function_name,
            config.federation_keys_upload_analytics_lambda_function_name
        ).forEach {
            backgroundActivities
                .invokeAnalyticsLogs(
                    scheduledEventTime = instantSetTo6am,
                    functionName = it
                )
                .requireBodyContains("CloudWatch Event triggered Lambda at wrong time")
        }
    }
}
