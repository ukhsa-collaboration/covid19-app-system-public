package smoke

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isIn
import com.natpryce.hamkrest.isNullOrEmptyString
import org.http4k.client.JavaHttpClient
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import smoke.actors.ApiVersion
import smoke.actors.MobileApp
import smoke.actors.MobileOS.Android
import smoke.actors.MobileOS.iOS
import smoke.env.SmokeTests
import uk.nhs.nhsx.core.DateFormatValidator

class MobileAppPollingSmokeTest {

    private val config = SmokeTests.loadConfig()
    private val client = JavaHttpClient()

    private val mobileApp = MobileApp(client, config)

    @Test
    fun `android mobile app polls availability`() {
        val json = MobileApp(client, config, Android).pollAvailability()

        JSONAssert.assertEquals(
            SmokeTests.loadStaticContent("availability-android.json"),
            json,
            JSONCompareMode.STRICT
        )
    }

    @Test
    fun `ios mobile app polls availability`() {
        val json = MobileApp(client, config, iOS).pollAvailability()

        JSONAssert.assertEquals(
            SmokeTests.loadStaticContent("availability-ios.json"),
            json,
            JSONCompareMode.STRICT
        )
    }

    @Test
    fun `mobile app polls exposure configuration`() {
        val json = MobileApp(client, config).pollExposureConfig()

        JSONAssert.assertEquals(
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

        venues.venues.forEach {
            assertThat(it.id, !isNullOrEmptyString)
            assertThat(it.riskyWindow.from, !isNullOrEmptyString)
            assertThat(it.riskyWindow.until, !isNullOrEmptyString)
            assertThat(DateFormatValidator.isValid(it.riskyWindow.from), equalTo(true))
            assertThat(DateFormatValidator.isValid(it.riskyWindow.until), equalTo(true))
        }
    }

    @Test
    fun `mobile app polls self isolation`() {
        val json = mobileApp.pollSelfIsolation()

        JSONAssert.assertEquals(
            SmokeTests.loadStaticContent("self-isolation.json"),
            json,
            JSONCompareMode.STRICT
        )
    }

    @Test
    fun `mobile app polls symptomatic questionnaire`() {
        val json = mobileApp.pollSymptomaticQuestionnaire()

        JSONAssert.assertEquals(
            SmokeTests.loadStaticContent("symptomatic-questionnaire.json"),
            json,
            JSONCompareMode.STRICT
        )
    }

    @Test
    fun `mobile app polls risky venues messages`() {
        val json = mobileApp.pollRiskyVenuesMessages()

        JSONAssert.assertEquals(
            SmokeTests.loadStaticContent("risky-venues-messages.json"),
            json,
            JSONCompareMode.STRICT
        )
    }
}
