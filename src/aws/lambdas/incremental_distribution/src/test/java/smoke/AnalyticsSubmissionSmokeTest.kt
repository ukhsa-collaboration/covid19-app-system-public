package smoke

import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.client.JavaHttpClient
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test
import smoke.clients.AnalyticsKeysSubmissionClient
import smoke.clients.AwsLambda
import smoke.clients.requireBodyText
import smoke.clients.requireStatusCode
import smoke.env.SmokeTests
import uk.nhs.nhsx.analyticssubmission.AnalyticsSubmissionHandlerTest.iOSPayloadFrom
import uk.nhs.nhsx.analyticssubmission.model.AnalyticsMetadata
import uk.nhs.nhsx.analyticssubmission.model.AnalyticsMetrics
import uk.nhs.nhsx.analyticssubmission.model.AnalyticsWindow
import uk.nhs.nhsx.analyticssubmission.model.ClientAnalyticsSubmissionPayload
import uk.nhs.nhsx.core.DateFormatValidator
import uk.nhs.nhsx.core.Jackson
import java.time.OffsetDateTime

class AnalyticsSubmissionSmokeTest {

    private val config = SmokeTests.loadConfig()
    private val client = JavaHttpClient()
    private val startDate = OffsetDateTime.now().minusDays(1).format(DateFormatValidator.formatter).toString()
    private val endDate = OffsetDateTime.now().plusDays(1).format(DateFormatValidator.formatter).toString()

    @Test
    fun `submit ios analytics data`() {
        val payload = generateIosAnalyticsPayload()
        val uploadResponse = submitAnalytics(Jackson.toJson(payload))
        assertThat(uploadResponse, hasStatus(Status.OK))
    }

    @Test
    fun `submit android analytics data`() {
        val payload = generateAndroidAnalyticsPayload()
        val response = submitAnalytics(Jackson.toJson(payload))
        assertThat(response, hasStatus(Status.OK))
    }

    @Test
    fun `invalid payload is rejected`() {
        val response = submitAnalytics(iOSPayloadFrom("1", "2", "3"))
        assertThat(response, hasStatus(Status.BAD_REQUEST))
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

    private fun submitAnalytics(json: String): Response = AnalyticsKeysSubmissionClient(client, config).upload(json)
}