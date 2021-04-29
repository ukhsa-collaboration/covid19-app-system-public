package uk.nhs.nhsx.core.events

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.Json
import uk.nhs.nhsx.core.headers.MobileAppVersion
import uk.nhs.nhsx.core.headers.MobileOS
import uk.nhs.nhsx.core.headers.MobileOSVersion
import uk.nhs.nhsx.core.headers.UserAgent

class IncomingHttpRequestTest {

    @Test
    fun `serializes to JSON correctly`() {
        assertThat(
            Json.toJson(
                IncomingHttpRequest(
                    "/path",
                    "GET",
                    201,
                    1234,
                    UserAgent(MobileAppVersion.Version(4, 3), MobileOS.Android, MobileOSVersion.of("29")),
                    "123-321",
                    "mobile",
                    "some message"
                )
            ),
            equalTo("""{"uri":"/path","method":"GET","status":201,"latency":1234,"userAgent":{"appVersion":{"major":4,"minor":3,"patch":0,"semVer":"4.3.0"},"os":"Android","osVersion":"29"},"requestId":"123-321","apiKey":"mobile","message":"some message"}""")
        )
    }
}
