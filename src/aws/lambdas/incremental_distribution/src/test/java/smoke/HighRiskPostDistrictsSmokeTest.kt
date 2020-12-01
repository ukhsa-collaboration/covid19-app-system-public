package smoke

import com.fasterxml.jackson.core.type.TypeReference
import org.assertj.core.api.Assertions.assertThat
import org.http4k.client.JavaHttpClient
import org.junit.jupiter.api.Test
import smoke.clients.RiskyPostDistrictsUploadClient
import smoke.clients.StaticContentClient
import smoke.env.SmokeTests
import uk.nhs.nhsx.core.SystemObjectMapper.MAPPER

class HighRiskPostDistrictsSmokeTest {

    private val config = SmokeTests.loadConfig()
    private val client = JavaHttpClient()

    private val staticContentClient = StaticContentClient(client, config)
    private val riskyPostDistrictsUploadClient = RiskyPostDistrictsUploadClient(client, config)

    @Test
    fun `uploads and downloads risky post districts`() {
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
        riskyPostDistrictsUploadClient.upload(json)

        // download v1
        val staticContentV1 = staticContentClient.riskyPostDistricts()
        val postDistrictsMapV1 = MAPPER.readValue(staticContentV1, object : TypeReference<Map<String, Any>>() {})
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
        val staticContentV2 = staticContentClient.riskyPostDistrictsV2()
        val postDistrictsMapV2 = MAPPER.readValue(staticContentV2, object : TypeReference<Map<String, Any>>() {})
        println(postDistrictsMapV2)

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

