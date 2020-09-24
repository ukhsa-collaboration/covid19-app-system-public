package smoke

import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.client.JavaHttpClient
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.hamkrest.hasStatus
import org.junit.Test
import smoke.clients.AnalyticsKeysSubmissionClient
import smoke.env.SmokeTests
import uk.nhs.nhsx.analyticssubmission.model.AnalyticsMetadata
import uk.nhs.nhsx.analyticssubmission.model.AnalyticsMetrics
import uk.nhs.nhsx.analyticssubmission.model.AnalyticsWindow
import uk.nhs.nhsx.analyticssubmission.model.ClientAnalyticsSubmissionPayload
import uk.nhs.nhsx.core.DateFormatValidator
import uk.nhs.nhsx.core.Jackson
import java.time.OffsetDateTime

class AnalyticsSubmissionSmokeTest() {
    private val config = SmokeTests.loadConfig()
    private val client = JavaHttpClient()
    private val startDate = OffsetDateTime.now().minusDays(1).format(DateFormatValidator.formatter).toString()
    private val endDate = OffsetDateTime.now().plusDays(1).format(DateFormatValidator.formatter).toString()

    @Test
    fun `submit ios analytics data`() {
        val payload: ClientAnalyticsSubmissionPayload = generateIosAnalyticsPayload()
        invokeAnalyticsAndValidate()
        val uploadResponse = uploadAnalyticsToS3(Jackson.toJson(payload))
        assertThat(uploadResponse, hasStatus(Status.OK))
    }

    @Test
    fun `submit android analytics data`() {
        val payload: ClientAnalyticsSubmissionPayload = generateAndroidAnalyticsPayload()
        invokeAnalyticsAndValidate()
        val response = uploadAnalyticsToS3(Jackson.toJson(payload))
        assertThat(response, hasStatus(Status.OK))
    }

    private fun generateIosAnalyticsPayload(): ClientAnalyticsSubmissionPayload {
        val analyticsMetrics = AnalyticsMetrics()
        val analyticsMetadata = AnalyticsMetadata("AB10", "iPhone-smoke-test", "iPhone OS 13.5.1 (17F80)", "3.0")

        return ClientAnalyticsSubmissionPayload(AnalyticsWindow(startDate, endDate), analyticsMetadata, analyticsMetrics, false)
    }

    private fun generateAndroidAnalyticsPayload(): ClientAnalyticsSubmissionPayload {
        val analyticsMetrics = AnalyticsMetrics()
        val analyticsMetadata = AnalyticsMetadata("AB10", "HUAWEI-smoke-test", "29", "3.0")
        return ClientAnalyticsSubmissionPayload(AnalyticsWindow(startDate, endDate), analyticsMetadata, analyticsMetrics, false)
    }

    private fun invokeAnalyticsAndValidate() {
        return AnalyticsKeysSubmissionClient(client, config).invokeMobileAnalytics()
    }

    private fun uploadAnalyticsToS3(json: String): Response {
        return AnalyticsKeysSubmissionClient(client, config).upload(json)
    }
}