package uk.nhs.nhsx.core.events

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import uk.nhs.nhsx.core.Json
import uk.nhs.nhsx.core.headers.MobileAppVersion
import uk.nhs.nhsx.core.headers.MobileOS
import uk.nhs.nhsx.core.headers.MobileOSVersion
import uk.nhs.nhsx.core.headers.UserAgent
import uk.nhs.nhsx.testhelper.assertions.isEqualToJson

class IncomingHttpRequestTest {

    @Test
    fun `serializes to JSON correctly`() {
        expectThat(
            Json.toJson(
                IncomingHttpRequest(
                    uri = "/path",
                    method = "GET",
                    status = 201,
                    latency = 1234,
                    userAgent = UserAgent(MobileAppVersion.Version(4, 3), MobileOS.Android, MobileOSVersion.of("29")),
                    requestId = "123-321",
                    apiKey = "mobile",
                    message = "some message"
                )
            )
        ).isEqualToJson("""
            {
                "uri": "/path",
                "method": "GET",
                "status": 201,
                "latency": 1234,
                "userAgent": {
                    "appVersion": {
                        "major": 4,
                        "minor": 3,
                        "patch": 0,
                        "semVer": "4.3.0"
                    },
                    "os": "Android",
                    "osVersion": "29"
                },
                "requestId": "123-321",
                "apiKey": "mobile",
                "message": "some message"
            }
        """.trimIndent())
    }
}
