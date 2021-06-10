package uk.nhs.nhsx.core.headers

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.Json
import uk.nhs.nhsx.core.headers.MobileAppVersion.Unknown
import uk.nhs.nhsx.core.headers.MobileAppVersion.Version
import uk.nhs.nhsx.core.headers.MobileOS.Android
import uk.nhs.nhsx.core.headers.MobileOS.iOS

class UserAgentTest {

    @Test
    fun `serializes to JSON correctly`() {
        assertThat(
            Json.toJson(UserAgent(Version(4, 3), Android, MobileOSVersion.of("29"))),
            equalTo("""{"appVersion":{"major":4,"minor":3,"patch":0,"semVer":"4.3.0"},"os":"Android","osVersion":"29"}""")
        )
    }

    @Test
    fun `extracts app version containing major and minor only`() {
        assertExtraction("p=Android,o=29,v=4.3,b=138", Version(4, 3), Android, MobileOSVersion.of("29"))
        assertExtraction("p=iOS,o=14.2,v=4.5,b=349", Version(4, 5), iOS, MobileOSVersion.of("14.2"))
    }

    @Test
    fun `extracts app os version`() {
        assertExtraction("p=Android,o=29,v=4.3,b=138", Version(4, 3), Android, MobileOSVersion.of("29"))
        assertExtraction("p=iOS,o=14.2,v=4.5,b=349", Version(4, 5), iOS, MobileOSVersion.of("14.2"))
    }

    @Test
    fun `extracts app version containing major, minor and patch`() {
        assertExtraction("p=Android,o=29,v=4.3.0,b=138", Version(4, 3, 0), Android, MobileOSVersion.of("29"))
        assertExtraction("p=iOS,o=14.2,v=4.5.1,b=349", Version(4, 5, 1), iOS, MobileOSVersion.of("14.2"))
    }

    @Test
    fun `handles invalid versions as unknown`() {
        assertExtraction("none", Unknown, null, null)
        assertExtraction("", Unknown, null, null)
        assertExtraction(".", Unknown, null, null)
        assertExtraction("=", Unknown, null, null)
        assertExtraction("p=Android,o=29,b=138", Unknown, Android, MobileOSVersion.of("29"))
        assertExtraction("p=Android,o=29,,b=138", Unknown, Android, MobileOSVersion.of("29"))
        assertExtraction("p=Android,o=29,v,b=138", Unknown, Android, MobileOSVersion.of("29"))
        assertExtraction("p=Android,o=29,v=,b=138", Unknown, Android, MobileOSVersion.of("29"))
        assertExtraction("p=Android,o=29,v==,b=138", Unknown, Android, MobileOSVersion.of("29"))
        assertExtraction("p=Android,o=29,v=.,b=138", Unknown, Android, MobileOSVersion.of("29"))
        assertExtraction("p=Android,o=29,v=4.,b=138", Unknown, Android, MobileOSVersion.of("29"))
        assertExtraction("p=Android,o=29,v=.4,b=138", Unknown, Android, MobileOSVersion.of("29"))
        assertExtraction("p=Android,o=29,v=4.0.,b=138", Unknown, Android, MobileOSVersion.of("29"))
        assertExtraction("p=Android,o=29,v=4.0.0.,b=138", Unknown, Android, MobileOSVersion.of("29"))
        assertExtraction("p=Android,o=29,v=4.0.0.0,b=138", Unknown, Android, MobileOSVersion.of("29"))
        assertExtraction("p=Android,o=29,v=4.a,b=138", Unknown, Android, MobileOSVersion.of("29"))
        assertExtraction("p=Android,o=29,v=4.0.0.a,b=138", Unknown, Android, MobileOSVersion.of("29"))
        assertExtraction("p=Android,o=29,v=a.4,b=138", Unknown, Android, MobileOSVersion.of("29"))
        assertExtraction("p=Android,o=29,v=a.b,b=138", Unknown, Android, MobileOSVersion.of("29"))
        assertExtraction("p=Android,o=29,v=a.b.c,b=138", Unknown, Android, MobileOSVersion.of("29"))
        assertExtraction("p=Android,o=29,v=abc,b=138", Unknown, Android, MobileOSVersion.of("29"))
        assertExtraction("p=Android,o=29,v=4=1.,b=138", Unknown, Android, MobileOSVersion.of("29"))
    }

    private fun assertExtraction(
        value: String,
        appVersion: MobileAppVersion,
        os: MobileOS?,
        osVersion: MobileOSVersion?
    ) {
        val agent = UserAgent.of(value)
        assertThat(agent.osVersion).isEqualTo(osVersion)
        assertThat(agent.appVersion).isEqualTo(appVersion)
        assertThat(agent.os).isEqualTo(os)
    }
}
