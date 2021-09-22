package smoke

import org.http4k.cloudnative.env.Environment
import org.http4k.core.Status.Companion.OK
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import smoke.actors.Analytics
import smoke.actors.MobileApp
import smoke.actors.MobileDeviceModel
import smoke.actors.createHandler
import smoke.env.SmokeTests
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import uk.nhs.nhsx.analyticssubmission.analyticsSubmissionAndroidComplete
import uk.nhs.nhsx.analyticssubmission.analyticsSubmissionIosComplete
import uk.nhs.nhsx.analyticssubmission.model.AnalyticsMetrics
import uk.nhs.nhsx.analyticssubmission.model.ClientAnalyticsSubmissionPayload
import uk.nhs.nhsx.core.Json
import uk.nhs.nhsx.core.headers.MobileOS
import uk.nhs.nhsx.core.headers.MobileOS.*
import uk.nhs.nhsx.testhelper.assertions.assertWithin
import java.time.Duration
import java.time.Duration.ofDays
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import org.http4k.format.Jackson as Http4kJackson

@ExtendWith(ApprovalTest::class)
class AnalyticsSubmissionSmokeTest {

    private val config = SmokeTests.loadConfig()
    private val client = createHandler(Environment.ENV)
    private val startDate = Instant.now().minus(ofDays(1)).truncatedTo(ChronoUnit.SECONDS)
    private val endDate = Instant.now().plus(ofDays(1)).truncatedTo(ChronoUnit.SECONDS)
    private val analytics = Analytics(config, client)

    @Test
    fun `submit ios analytics data`(approver: Approver) {
        val deviceModel = MobileDeviceModel(UUID.randomUUID().toString())
        val mobileApp = MobileApp(client, config, os = iOS, model = deviceModel)

        expectThat(mobileApp.submitAnalyticsKeys(startDate = startDate, endDate = endDate))
            .isEqualTo(OK)

        approver.assertAthenaQueryReturnsCorrect(deviceModel, metrics(iOS))
    }

    @Test
    fun `submit android analytics data`(approver: Approver) {
        val deviceModel = MobileDeviceModel(UUID.randomUUID().toString())
        val mobileApp = MobileApp(client, config, os = Android, model = deviceModel)

        expectThat(mobileApp.submitAnalyticsKeys(startDate = startDate, endDate = endDate))
            .isEqualTo(OK)

        approver.assertAthenaQueryReturnsCorrect(deviceModel, metrics(Android))
    }

    private fun metrics(os: MobileOS): AnalyticsMetrics {
        val json = when (os) {
            Android -> analyticsSubmissionAndroidComplete()
            iOS -> analyticsSubmissionIosComplete()
            Unknown -> throw RuntimeException("Unknown device os. Set one for ${this::class.java.simpleName}?")
        }
        val clientPayload = Json.readJsonOrThrow<ClientAnalyticsSubmissionPayload>(json)
        return clientPayload.metrics
    }

    private fun Approver.assertAthenaQueryReturnsCorrect(
        deviceModel: MobileDeviceModel,
        analyticsMetrics: AnalyticsMetrics
    ) {
        assertWithin(Duration.ofSeconds(config.analytics_submission_ingestion_interval.toLong() * 4)) {
            val dataFromAthena = analytics.getRecordedAnalyticsFor(deviceModel)

            val fieldsAndValuesWeSent = Http4kJackson.fields(Http4kJackson.asJsonObject(analyticsMetrics))
                .map { it.first.lowercase(Locale.getDefault()) to it.second.toString() }

            val interestingFieldsFromAthena =
                dataFromAthena.filter { it.first in fieldsAndValuesWeSent.map(Pair<String, String>::first) }

            val interestingFieldsFromAthenaCsv =
                interestingFieldsFromAthena.joinToString("\n") { it.first + "," + it.second }

            assertApproved(interestingFieldsFromAthenaCsv)
        }
    }

}
