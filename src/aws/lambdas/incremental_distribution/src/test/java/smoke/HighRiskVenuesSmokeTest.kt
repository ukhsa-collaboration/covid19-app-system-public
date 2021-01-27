package smoke

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.client.JavaHttpClient
import org.junit.jupiter.api.Test
import smoke.actors.MobileApp
import smoke.actors.RiskParties
import smoke.env.SmokeTests
import uk.nhs.nhsx.core.DateFormatValidator
import uk.nhs.nhsx.highriskvenuesupload.model.HighRiskVenue
import uk.nhs.nhsx.highriskvenuesupload.model.HighRiskVenues
import uk.nhs.nhsx.highriskvenuesupload.model.RiskyWindow
import java.time.OffsetDateTime
import java.util.Random

class HighRiskVenuesSmokeTest {

    private val config = SmokeTests.loadConfig()
    private val client = JavaHttpClient()

    private val mobileApp = MobileApp(client, config)
    private val riskParties = RiskParties(client, config)

    @Test
    fun `mobile app requests venue circuit break based on risky venues`() {
        val expectedRiskyVenues = generateRiskyVenues()

        // upload
        riskParties.uploadsRiskyVenues(generateCsvFrom(expectedRiskyVenues))

        // download
        val highRiskVenues = mobileApp.pollRiskyVenues()
        assertThat(highRiskVenues.venues, equalTo(expectedRiskyVenues.venues))

        mobileApp.venueCircuitBreaker.requestAndApproveCircuitBreak()
    }

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

    private fun generateCsvFrom(highRiskVenues: HighRiskVenues): String {
        val csvRows = highRiskVenues.venues
            .joinToString(separator = "\n") { """"${it.id}", "${it.riskyWindow.from}", "${it.riskyWindow.until}"""" }

        return """# venue_id, start_time, end_time
            |$csvRows
            """.trimMargin()
    }
}

