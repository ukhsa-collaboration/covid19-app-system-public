package smoke

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.client.JavaHttpClient
import org.junit.jupiter.api.Test
import smoke.actors.MobileApp
import smoke.actors.RiskParties
import smoke.data.RiskPartyData.generateCsvFrom
import smoke.data.RiskPartyData.generateRiskyVenues
import smoke.env.SmokeTests

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
}

