package smoke

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.isIn
import com.natpryce.hamkrest.isNullOrEmptyString
import com.natpryce.hamkrest.present
import org.http4k.client.JavaHttpClient
import org.http4k.core.Status
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert.assertEquals
import org.skyscreamer.jsonassert.JSONCompareMode
import smoke.actors.ApiVersion
import smoke.actors.MobileApp
import smoke.actors.requireBodyText
import smoke.actors.requireStatusCode
import smoke.env.SmokeTests
import uk.nhs.nhsx.core.headers.MobileOS.Android
import uk.nhs.nhsx.core.headers.MobileOS.iOS

class MobileAppPollingSmokeTest {

    private val config = SmokeTests.loadConfig()
    private val client = JavaHttpClient()

    private val mobileApp = MobileApp(client, config)

    @Test
    fun `android mobile app polls availability`() {
        val json = MobileApp(client, config, Android).pollAvailability()

        assertEquals(
            SmokeTests.loadStaticContent("availability-android.json"),
            json,
            JSONCompareMode.STRICT
        )
    }

    @Test
    fun `ios mobile app polls availability`() {
        val json = MobileApp(client, config, iOS).pollAvailability()

        assertEquals(
            SmokeTests.loadStaticContent("availability-ios.json"),
            json,
            JSONCompareMode.STRICT
        )
    }

    @Test
    fun `mobile app polls exposure configuration`() {
        val json = MobileApp(client, config).pollExposureConfig()

        assertEquals(
            SmokeTests.loadStaticContent("exposure-configuration.json"),
            json,
            JSONCompareMode.STRICT
        )
    }

    @Disabled
    @Test //FIXME no risky post district json available in fresh environment without uploading it first via API
    fun `gets risky postal districts`() {
        val postalDistricts = mobileApp.pollRiskyPostcodes(ApiVersion.V1)

        postalDistricts.entries.forEach {
            assertThat(it.key, !isNullOrEmptyString)
            assertThat(it.value, isIn("L", "M", "H"))
        }
    }

    @Test
    fun `mobile app polls risky venues`() {
        val venues = mobileApp.pollRiskyVenues()
        assertThat(venues.venues, present())
    }

    @Test
    fun `mobile app polls self isolation`() {
        val json = mobileApp.pollSelfIsolation()

        assertEquals(
            SmokeTests.loadStaticContent("self-isolation.json"),
            json,
            JSONCompareMode.STRICT
        )
    }

    @Test
    fun `mobile app polls symptomatic questionnaire`() {
        val json = mobileApp.pollSymptomaticQuestionnaire()

        assertEquals(
            SmokeTests.loadStaticContent("symptomatic-questionnaire.json"),
            json,
            JSONCompareMode.STRICT
        )
    }

    @Test
    fun `mobile app polls risky venue configuration`() {
        val json = mobileApp.pollRiskyVenueConfiguration()

        assertEquals(
            SmokeTests.loadStaticContent("risky-venue-configuration.json"),
            json,
            JSONCompareMode.STRICT
        )
    }

    @Test
    fun `mobile app polls empty submission`() {
        mobileApp.emptySubmission()
            .requireStatusCode(Status.OK)
            .requireBodyText("")
    }

    @Test
    fun `mobile app polls empty submission v2`() {
        mobileApp.emptySubmissionV2()
            .requireStatusCode(Status.OK)
            .requireBodyText("")
    }
}
