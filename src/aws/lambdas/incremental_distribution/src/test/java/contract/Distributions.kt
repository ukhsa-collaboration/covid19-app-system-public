package contract

import contract.infra.BackendContractScenario
import contract.infra.RecordingTest
import contract.infra.ReplayTest
import org.junit.jupiter.api.Test
import smoke.actors.ApiVersion
import smoke.actors.MobileApp
import smoke.actors.RiskParties
import smoke.data.RiskPartyData
import smoke.env.SmokeTests

interface Distributions : BackendContractScenario {
    @Test
    @JvmDefault
    fun `Mobile app polls for exposure config`() {
        control.addNote("Mobile app polls for exposure config")
        MobileApp(mitmHttpClient(), envConfig).pollExposureConfig()
    }

    @Test
    @JvmDefault
    fun `Mobile app polls for availability`() {
        control.addNote("Mobile app polls for availability")
        MobileApp(mitmHttpClient(), envConfig).pollAvailability()
    }

    @Test
    @JvmDefault
    fun `Mobile app polls for risky venue info`() {
        RiskParties(http(), envConfig).uploadsRiskyVenues(RiskPartyData.generateCsvFrom(RiskPartyData.generateRiskyVenues()))

        control.addNote("Mobile app polls risky venue info")
        MobileApp(mitmHttpClient(), envConfig).pollRiskyVenues()
    }

    @Test
    @JvmDefault
    fun `Mobile app polls for risky postcodes v1`() {
        RiskParties(http(), envConfig).uploadsRiskyPostcodes(RiskPartyData.generateRiskyPostcodes())

        control.addNote("Mobile app polls risky postcodes v1")
        MobileApp(mitmHttpClient(), envConfig).pollRiskyPostcodes(ApiVersion.V1)
    }

    @Test
    @JvmDefault
    fun `Mobile app polls for risky postcodes v2`() {
        RiskParties(http(), envConfig).uploadsRiskyPostcodes(RiskPartyData.generateRiskyPostcodes())

        control.addNote("Mobile app polls risky postcodes v2")
        MobileApp(mitmHttpClient(), envConfig).pollRiskyPostcodes(ApiVersion.V2)
    }

    @Test
    @JvmDefault
    fun `Mobile app polls risky venue messages`() {
        RiskParties(http(), envConfig).uploadsRiskyPostcodes(RiskPartyData.generateRiskyPostcodes())

        control.addNote("Mobile app polls risky venue messages")
        MobileApp(mitmHttpClient(), envConfig).pollRiskyVenuesMessages()
    }

    @Test
    @JvmDefault
    fun `Mobile app polls for symptom questionnaire`() {
        control.addNote("Mobile app polls for symptom questionnaire")
        MobileApp(mitmHttpClient(), envConfig).pollSymptomaticQuestionnaire()
    }

    @Test
    @JvmDefault
    fun `Mobile app polls for self-isolation data`() {
        control.addNote("Mobile app polls for self-isolation data")
        MobileApp(mitmHttpClient(), envConfig).pollSelfIsolation()
    }

}

class RecordingDistributionsTest : RecordingTest(), Distributions {
    override val envConfig = SmokeTests.loadConfig()
}

class ReplayDistributionsTest : ReplayTest(), Distributions {
    override val envConfig = SmokeTests.loadConfig()
}