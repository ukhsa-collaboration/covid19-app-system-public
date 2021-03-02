package smoke.data

import uk.nhs.nhsx.core.DateFormatValidator
import uk.nhs.nhsx.highriskvenuesupload.model.HighRiskVenue
import uk.nhs.nhsx.highriskvenuesupload.model.HighRiskVenues
import uk.nhs.nhsx.highriskvenuesupload.model.RiskyWindow
import java.time.OffsetDateTime
import java.util.Random

object RiskPartyData {
    fun generateRiskyPostcodes() = """{
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

    fun generateRiskyVenues(numberOfVenues: Int = 3): HighRiskVenues {
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

    fun generateCsvFrom(highRiskVenues: HighRiskVenues): String {
        val csvRows = highRiskVenues.venues
            .joinToString(separator = "\n") { """"${it.id}", "${it.riskyWindow.from}", "${it.riskyWindow.until}"""" }

        return """# venue_id, start_time, end_time
            |$csvRows
            """.trimMargin()
    }
}