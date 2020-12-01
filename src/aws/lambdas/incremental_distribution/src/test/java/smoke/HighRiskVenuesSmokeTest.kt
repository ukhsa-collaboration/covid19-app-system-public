package smoke

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isNullOrEmptyString
import org.http4k.client.JavaHttpClient
import org.junit.jupiter.api.Test
import smoke.clients.RiskyVenuesUploadClient
import smoke.clients.StaticContentClient
import smoke.clients.VenuesCircuitBreakerClient
import smoke.env.SmokeTests
import uk.nhs.nhsx.core.DateFormatValidator
import uk.nhs.nhsx.core.Jackson
import uk.nhs.nhsx.highriskvenuesupload.model.HighRiskVenue
import uk.nhs.nhsx.highriskvenuesupload.model.HighRiskVenues
import uk.nhs.nhsx.highriskvenuesupload.model.RiskyWindow
import java.time.OffsetDateTime
import java.util.*

class HighRiskVenuesSmokeTest {

    private val config = SmokeTests.loadConfig()
    private val client = JavaHttpClient()

    private val staticContentClient = StaticContentClient(client, config)
    private val riskyVenuesUploadClient = RiskyVenuesUploadClient(client, config)
    private val venuesCircuitBreakerClient = VenuesCircuitBreakerClient(client, config)

    @Test
    fun `upload, download, circuit breaker`() {
        val expectedRiskyVenues = generateRiskyVenues()

        // upload
        val csv = generateCsvFrom(expectedRiskyVenues)
        riskyVenuesUploadClient.upload(csv)

        // download
        val staticContentRiskyVenues = staticContentClient.riskyVenues()
        val highRiskVenues = deserialize(staticContentRiskyVenues)
        assertThat(highRiskVenues?.venues, equalTo(expectedRiskyVenues.venues))

        // circuit breaker request
        val tokenResponse = venuesCircuitBreakerClient.requestCircuitBreaker()
        assertThat(tokenResponse.approval, equalTo("yes"))
        assertThat(tokenResponse.approvalToken, !isNullOrEmptyString)

        // circuit breaker approval
        val resolutionResponse = venuesCircuitBreakerClient.resolutionCircuitBreaker(tokenResponse)
        assertThat(resolutionResponse.approval, equalTo("yes"))
    }

   /* @Test Commented out until feature implemented
    fun `upload, download, circuit breaker with MessageType`() {
        val expectedRiskyVenues: HighRiskVenues = generateRiskyVenuesWithMessageType()

        // upload
        val csv = generateCsvFromMessageType(expectedRiskyVenues)
        riskyVenuesUploadClient.upload(csv)

        // download
        val staticContentRiskyVenues = staticContentClient.riskyVenues()
        val highRiskVenues = deserialize(staticContentRiskyVenues)
        assertThat(highRiskVenues?.venues, equalTo(expectedRiskyVenues.venues))


        // circuit breaker
        val circuitBreakerRequest = RiskyVenueCircuitBreakerRequest(expectedRiskyVenues.venues.first().id)
        val tokenResponse = venuesCircuitBreakerClient.requestCircuitBreaker(circuitBreakerRequest)
        assertThat(tokenResponse.approval, equalTo("yes"))
        assertThat(tokenResponse.approvalToken, !isNullOrEmptyString)
    }*/
    private fun generateRiskyVenues(numberOfVenues: Int = 3): HighRiskVenues {
        val validChars = "CDEFHJKMPRTVWXY2345689"
        val venuesList = (0 until numberOfVenues)
            .map {
                val venueId = (0 until 12).map { validChars[Random().nextInt(validChars.length)] }.joinToString(separator = "")
                val startTime = OffsetDateTime.now().minusDays(1).format(DateFormatValidator.formatter)
                val endTime = OffsetDateTime.now().plusDays(1).format(DateFormatValidator.formatter)
                HighRiskVenue(venueId, RiskyWindow(startTime, endTime))
            }
        return HighRiskVenues(venuesList)
    }

    private fun generateRiskyVenuesWithMessageType(numberOfVenues: Int = 3): HighRiskVenues {
        val validChars = "CDEFHJKMPRTVWXY2345689"

        val venuesList = (0 until numberOfVenues)
            .map {
                val venueId = (0 until 12).map { validChars[Random().nextInt(validChars.length)] }.joinToString(separator = "")
                val startTime = OffsetDateTime.now().minusDays(1).format(DateFormatValidator.formatter)
                val endTime = OffsetDateTime.now().plusDays(1).format(DateFormatValidator.formatter)
                val messageType = "M" + (1..3).random()
                val optionalParameter = if(messageType == "M3") { "0" + (0 until 11).map { (0..9).random() }.joinToString(separator = "") } else { "" }

                HighRiskVenue(venueId, RiskyWindow(startTime, endTime), messageType, optionalParameter)
            }
        return HighRiskVenues(venuesList)
    }

    private fun generateCsvFrom(highRiskVenues: HighRiskVenues): String {
        val csvRows = highRiskVenues.venues
            .joinToString(separator = "\n") { """"${it.id}", "${it.riskyWindow.from}", "${it.riskyWindow.until}"""" }

        return """# venue_id, start_time, end_time
            |$csvRows
            """.trimMargin()
    }
    private fun generateCsvFromMessageType(highRiskVenues: HighRiskVenues): String {
        val csvRows = highRiskVenues.venues
            .joinToString(separator = "\n") { """"${it.id}", "${it.riskyWindow.from}", "${it.riskyWindow.until}", "${it.messageType}", "${it.optionalParameter}"""" }
        print(csvRows)
        return """# venue_id, start_time, end_time, message_type, optional_parameter
            |$csvRows
            """.trimMargin()
    }
    private fun deserializeOrThrow(staticContentRiskyVenues: String) =
        Jackson.deserializeMaybe(staticContentRiskyVenues, HighRiskVenues::class.java)
            .orElseThrow { IllegalStateException("Unable to deserialize venues response") }


    private fun deserialize(staticContentRiskyVenues: String): HighRiskVenues? {
        val riskyVenueMapper = ObjectMapper()
            .deactivateDefaultTyping()
            .registerModule(ParameterNamesModule(JsonCreator.Mode.PROPERTIES))
            .registerModule(Jdk8Module())
        return riskyVenueMapper.readValue(staticContentRiskyVenues, HighRiskVenues::class.java)
    }

}

