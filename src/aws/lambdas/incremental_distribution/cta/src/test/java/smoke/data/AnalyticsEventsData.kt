package smoke.data

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.ISODateTimeFormat
import java.util.*

object AnalyticsEventsData {
    private val fmt : DateTimeFormatter = ISODateTimeFormat.dateTimeNoMillis()
    private val default_ts: DateTime = fmt.parseDateTime("2020-08-24T21:59:00Z")

    fun analyticsEvents(osVersion: UUID) = analyticsEvents(osVersion.toString())

    fun analyticsEvents(osVersion: String, ts: Optional<DateTime> = Optional.empty()) = """
        {
            "metadata": {
                "operatingSystemVersion": "$osVersion",
                "latestApplicationVersion": "3.0",
                "deviceModel": "iPhone11,2",
                "postalDistrict": "A1"
            },
            "events": [
                {
                    "type": "exposure_window",
                    "version": 1,
                    "payload": {
                        "date": "${fmt.print(ts.orElse(default_ts))}",
                        "infectiousness": "high|none|standard",
                        "scanInstances": [
                            {
                                "minimumAttenuation": 1,
                                "secondsSinceLastScan": 5,
                                "typicalAttenuation": 2
                            }
                        ],
                        "riskScore": 1.2
                    }
                }
            ]
        }
    """

}
