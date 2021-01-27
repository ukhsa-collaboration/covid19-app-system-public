package smoke

import org.assertj.core.api.Assertions.assertThat
import org.http4k.client.JavaHttpClient
import org.junit.jupiter.api.Test
import smoke.actors.MobileApp
import smoke.actors.RiskParties
import smoke.actors.ApiVersion.V1
import smoke.actors.ApiVersion.V2
import smoke.env.SmokeTests

class HighRiskPostDistrictsSmokeTest {

    private val config = SmokeTests.loadConfig()
    private val client = JavaHttpClient()

    private val mobileApp = MobileApp(client, config)
    private val riskParties = RiskParties(client, config)

    @Test
    fun `mobile app can download updated risky postcodes`() {
        val json = """{
          "postDistricts": {
            "AB10": {
              "riskIndicator": "L",
              "tierIndicator": "EN.Tier1"
            },
            "AB11": {
              "riskIndicator": "H",
              "tierIndicator": "EN.Tier3"
            },
            "AB12": {
              "riskIndicator": "L",
              "tierIndicator": "EN.Tier1"
            },
            "AB15": {
              "riskIndicator": "M",
              "tierIndicator": "EN.Tier2"
            }
          },
          "localAuthorities": {
            "A1": {
              "tierIndicator": "EN.Tier3"
            },
            "A2": {
              "tierIndicator": "EN.Tier1"
            }, 
            "A3": {
              "tierIndicator": "EN.Tier2"
            }
          }
        }""".trimIndent()
        riskParties.uploadsRiskyPostcodes(json)

        // download v1
        val postDistrictsMapV1 = mobileApp.pollRiskyPostcodes(V1)
        assertThat(postDistrictsMapV1).isEqualTo(
            mapOf("postDistricts" to
                mapOf(
                    "AB10" to "L",
                    "AB11" to "H",
                    "AB12" to "L",
                    "AB15" to "M"
                )
            )
        )

        // download v2
        val postDistrictsMapV2 = mobileApp.pollRiskyPostcodes(V2)

        assertThat(postDistrictsMapV2["postDistricts"]).isEqualTo(
            mapOf(
                "AB10" to "EN.Tier1",
                "AB11" to "EN.Tier3",
                "AB12" to "EN.Tier1",
                "AB15" to "EN.Tier2"
            )
        )

        assertThat(postDistrictsMapV2["localAuthorities"]).isEqualTo(
            mapOf(
                "A1" to "EN.Tier3",
                "A2" to "EN.Tier1",
                "A3" to "EN.Tier2"
            )
        )

        assertThat(postDistrictsMapV2["riskLevels"]).isNotNull
    }

}

