package smoke

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isNullOrEmptyString
import org.http4k.client.JavaHttpClient
import org.junit.Test
import smoke.clients.RiskyVenuesUploadClient
import smoke.clients.StaticContentClient
import smoke.clients.VenuesCircuitBreakerClient
import smoke.env.SmokeTests
import uk.nhs.nhsx.circuitbreakers.RiskyVenueCircuitBreakerRequest
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
        val expectedRiskyVenues: HighRiskVenues = generateRiskyVenues()

        // upload
        val csv = generateCsvFrom(expectedRiskyVenues)
        riskyVenuesUploadClient.upload(csv)

        // download
        val staticContentRiskyVenues = staticContentClient.riskyVenues()
        val highRiskVenues = deserializeOrThrow(staticContentRiskyVenues)
        assertThat(highRiskVenues.venues, equalTo(expectedRiskyVenues.venues))

        // circuit breaker
        val circuitBreakerRequest = RiskyVenueCircuitBreakerRequest(expectedRiskyVenues.venues.first().id)
        val tokenResponse = venuesCircuitBreakerClient.requestCircuitBreaker(circuitBreakerRequest)
        assertThat(tokenResponse.approval, equalTo("yes"))
        assertThat(tokenResponse.approvalToken, !isNullOrEmptyString)
    }

    private fun generateRiskyVenues(numberOfVenues: Int = 3): HighRiskVenues {
        val venuesList = (0 until numberOfVenues)
            .map {
                val venueId = UUID.randomUUID().toString()
                val startTime = OffsetDateTime.now().minusDays(1).format(DateFormatValidator.formatter)
                val endTime = OffsetDateTime.now().plusDays(1).format(DateFormatValidator.formatter)
                HighRiskVenue(venueId, RiskyWindow(startTime, endTime))
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

    private fun deserializeOrThrow(staticContentRiskyVenues: String) =
        Jackson.deserializeMaybe(staticContentRiskyVenues, HighRiskVenues::class.java)
            .orElseThrow { IllegalStateException("Unable to deserialize venues response") }

}