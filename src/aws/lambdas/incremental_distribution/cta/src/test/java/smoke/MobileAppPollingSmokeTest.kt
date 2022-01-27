package smoke

import com.fasterxml.jackson.module.kotlin.readValue
import org.http4k.cloudnative.env.Environment
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import smoke.actors.ApiVersion
import smoke.actors.MobileApp
import smoke.actors.createHandler
import smoke.env.SmokeTests
import smoke.env.SmokeTests.loadStaticContent
import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.containsKeys
import strikt.assertions.isA
import strikt.assertions.isEmpty
import strikt.assertions.isGreaterThanOrEqualTo
import strikt.assertions.isNotEmpty
import strikt.assertions.matches
import uk.nhs.nhsx.core.headers.MobileOS.Android
import uk.nhs.nhsx.core.headers.MobileOS.iOS
import uk.nhs.nhsx.highriskvenuesupload.model.HighRiskVenues
import uk.nhs.nhsx.localstats.LocalStatsJson
import uk.nhs.nhsx.localstats.domain.AreaCode
import uk.nhs.nhsx.localstats.domain.DailyLocalStatsDocument
import uk.nhs.nhsx.testhelper.assertions.bodyString
import uk.nhs.nhsx.testhelper.assertions.hasStatus
import uk.nhs.nhsx.testhelper.assertions.isEqualToJson
import uk.nhs.nhsx.testhelper.assertions.key
import uk.nhs.nhsx.testhelper.assertions.value

class MobileAppPollingSmokeTest {

    private val config = SmokeTests.loadConfig()
    private val client = createHandler(Environment.ENV)
    private val mobileApp = MobileApp(client, config)

    @Test
    fun `android mobile app polls availability`() {
        val json = MobileApp(client, config, Android).pollAvailability()

        expectThat(json).isEqualToJson(loadStaticContent("availability-android.json"))
    }

    @Test
    fun `ios mobile app polls availability`() {
        val json = MobileApp(client, config, iOS).pollAvailability()

        expectThat(json).isEqualToJson(loadStaticContent("availability-ios.json"))
    }

    @Test
    fun `mobile app polls exposure configuration`() {
        val json = MobileApp(client, config).pollExposureConfig()

        expectThat(json).isEqualToJson(loadStaticContent("exposure-configuration.json"))
    }

    @Disabled
    @Test //FIXME no risky post district json available in fresh environment without uploading it first via API
    fun `gets risky postal districts`() {
        val postalDistricts = mobileApp.pollRiskyPostcodes(ApiVersion.V1)

        expectThat(postalDistricts.entries).all {
            key.isNotEmpty()
            value.isA<String>().matches(Regex("[LMH]"))
        }
    }

    @Test
    fun `mobile app polls risky venues`() {
        val venues = mobileApp.pollRiskyVenues()

        expectThat(venues)
            .get(HighRiskVenues::venues)
            .get("size") { size }
            .isGreaterThanOrEqualTo(0)
    }

    @Test
    fun `mobile app polls self isolation`() {
        val json = mobileApp.pollSelfIsolation()

        expectThat(json).isEqualToJson(loadStaticContent("self-isolation.json"))
    }

    @Test
    fun `mobile app polls symptomatic questionnaire`() {
        val json = mobileApp.pollSymptomaticQuestionnaire()

        expectThat(json).isEqualToJson(loadStaticContent("symptomatic-questionnaire.json"))
    }

    @Test
    fun `mobile app polls risky venue configuration`() {
        val json = mobileApp.pollRiskyVenueConfiguration()

        expectThat(json).isEqualToJson(loadStaticContent("risky-venue-configuration.json"))
    }

    @Test
    fun `mobile app polls local stats`() {
        val json = mobileApp.pollLocalStats()
        val document: DailyLocalStatsDocument = LocalStatsJson.mapper.readValue(json)

        expectThat(document.lowerTierLocalAuthorities)
            .containsKeys(
                AreaCode.of("E06000001"),
                AreaCode.of("E06000002"),
            )
    }

    @Test
    fun `mobile app polls empty submission`() {
        expectThat(mobileApp.emptySubmission()) {
            hasStatus(OK)
            bodyString.isEmpty()
        }
    }

    @Test
    fun `mobile app polls empty submission v2`() {
        expectThat(mobileApp.emptySubmissionV2()) {
            hasStatus(OK)
            bodyString.isEmpty()
        }
    }
}
