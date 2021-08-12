package smoke.data

import java.util.*

object AnalyticsEventsData {
    fun analyticsEvents(osVersion: UUID) = """
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
                        "date": "2020-08-24T21:59:00Z",
                        "infectiousness": "high|none|standard",
                        "scanInstances": [
                            {
                                "minimumAttenuation": 1,
                                "secondsSinceLastScan": 5,
                                "typicalAttenuation": 2
                            }
                        ],
                        "riskScore": "FIXME: sample int value (range?) or string value (enum?)"
                    }
                }
            ]
        }
    """

}
