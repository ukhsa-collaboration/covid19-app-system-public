package smoke.data

import uk.nhs.nhsx.highriskvenuesupload.model.HighRiskVenue
import uk.nhs.nhsx.highriskvenuesupload.model.HighRiskVenues
import uk.nhs.nhsx.highriskvenuesupload.model.MessageType
import uk.nhs.nhsx.highriskvenuesupload.model.RiskyWindow
import uk.nhs.nhsx.highriskvenuesupload.model.VenueId
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit.SECONDS
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
                val startTime = Instant.now().minus(Duration.ofDays(1)).truncatedTo(SECONDS)
                val endTime = Instant.now().plus(Duration.ofDays(1)).truncatedTo(SECONDS)
                HighRiskVenue(VenueId.of(venueId), RiskyWindow(startTime, endTime), MessageType("M1"))
            }
        return HighRiskVenues(venuesList)
    }

    fun generateCsvFrom(highRiskVenues: HighRiskVenues): String {
        val csvRows = highRiskVenues.venues
            .joinToString(separator = "\n") { """"${it.id}", "${it.riskyWindow.from}", "${it.riskyWindow.until}", "${it.messageType}", "${it.optionalParameter ?: ""}" """ }

        return """# venue_id, start_time, end_time, message_type, optional_parameter
            |$csvRows
            """.trimMargin()
    }
}
