package smoke

import org.http4k.cloudnative.env.Environment
import org.junit.jupiter.api.Test
import smoke.actors.MobileApp
import smoke.actors.RiskParties
import smoke.actors.createHandler
import smoke.data.RiskPartyData.generateCsvFrom
import smoke.data.RiskPartyData.generateRiskyVenues
import smoke.env.SmokeTests
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import uk.nhs.nhsx.highriskvenuesupload.model.HighRiskVenues

class HighRiskVenuesSmokeTest {

    private val config = SmokeTests.loadConfig()
    private val client = createHandler(Environment.ENV)

    private val mobileApp = MobileApp(client, config)
    private val riskParties = RiskParties(client, config)

    @Test
    fun `mobile app requests venue circuit break based on risky venues`() {
        val expectedRiskyVenues = generateRiskyVenues()

        // upload
        riskParties.uploadsRiskyVenues(generateCsvFrom(expectedRiskyVenues))

        // download
        val highRiskVenues = mobileApp.pollRiskyVenues()
        expectThat(highRiskVenues)
            .get(HighRiskVenues::venues)
            .isEqualTo(expectedRiskyVenues.venues)

        mobileApp.venueCircuitBreaker.requestAndApproveCircuitBreak()
    }
}

