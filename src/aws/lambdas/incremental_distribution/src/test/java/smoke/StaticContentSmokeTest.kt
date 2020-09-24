package smoke

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isIn
import com.natpryce.hamkrest.isNullOrEmptyString
import org.assertj.core.api.Assertions.fail
import org.http4k.client.JavaHttpClient
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import smoke.clients.StaticContentClient
import smoke.env.SmokeTests
import uk.nhs.nhsx.core.DateFormatValidator
import uk.nhs.nhsx.core.Jackson
import uk.nhs.nhsx.highriskpostcodesupload.RiskyPostCodes
import uk.nhs.nhsx.highriskvenuesupload.model.HighRiskVenues

class StaticContentSmokeTest {

    private val config = SmokeTests.loadConfig()
    private val client = JavaHttpClient()

    private val staticContentClient = StaticContentClient(client, config)

    @Test
    fun `gets availability android`() {
        val json = staticContentClient.availabilityAndroid()

        JSONAssert.assertEquals(
            SmokeTests.loadStaticContent("availability-android.json"),
            json,
            JSONCompareMode.STRICT
        )
    }

    @Test
    fun `gets availability ios`() {
        val json = staticContentClient.availabilityIos()

        JSONAssert.assertEquals(
            SmokeTests.loadStaticContent("availability-ios.json"),
            json,
            JSONCompareMode.STRICT
        )
    }

    @Test
    fun `gets exposure configuration`() {
        val json = staticContentClient.exposureConfiguration()

        JSONAssert.assertEquals(
            SmokeTests.loadStaticContent("exposure-configuration.json"),
            json,
            JSONCompareMode.STRICT
        )
    }

    //@Test //FIXME no risky post district json available in fresh environment without uploading it first via API
    fun `gets risky postal districts`() {
        val json = staticContentClient.riskyPostDistricts()
        // cannot compare against static file because it might change
        val postalDistricts = Jackson.deserializeMaybe(json, RiskyPostCodes::class.java)
            .orElseGet { fail("Unable to deserialize postal districts: $json") }

        postalDistricts.postDistricts.entries.forEach {
            assertThat(it.key, !isNullOrEmptyString)
            assertThat(it.value, isIn("L", "M", "H"))
        }
    }

    @Test
    fun `gets risky venues`() {
        val json = staticContentClient.riskyVenues()
        // cannot compare against static file because it might change
        val venues = deserialize(json)

        venues!!.venues.forEach {
            assertThat(it.id, !isNullOrEmptyString)
            assertThat(it.riskyWindow.from, !isNullOrEmptyString)
            assertThat(it.riskyWindow.until, !isNullOrEmptyString)
            assertThat(DateFormatValidator.isValid(it.riskyWindow.from), equalTo(true))
            assertThat(DateFormatValidator.isValid(it.riskyWindow.until), equalTo(true))
        }
    }

    @Test
    fun `gets self isolation`() {
        val json = staticContentClient.selfIsolation()

        JSONAssert.assertEquals(
            SmokeTests.loadStaticContent("self-isolation.json"),
            json,
            JSONCompareMode.STRICT
        )
    }

    @Test
    fun `gets symptomatic questionnaire`() {
        val json = staticContentClient.symptomaticQuestionnaire()

        JSONAssert.assertEquals(
            SmokeTests.loadStaticContent("symptomatic-questionnaire.json"),
            json,
            JSONCompareMode.STRICT
        )
    }

    private fun deserialize(staticContentRiskyVenues: String): HighRiskVenues? {
        val riskyVenueMapper = ObjectMapper()
            .deactivateDefaultTyping()
            .registerModule(ParameterNamesModule(JsonCreator.Mode.PROPERTIES))
            .registerModule(Jdk8Module())
        return riskyVenueMapper.readValue(staticContentRiskyVenues, HighRiskVenues::class.java)
    }
}