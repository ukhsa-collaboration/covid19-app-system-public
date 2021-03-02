package uk.nhs.nhsx.core.headers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.headers.MobileAppVersion.*

class UserAgentTest {

    @Test
    fun `extracts app version containing major and minor only`() {
        assertThat(UserAgent("p=Android,o=29,v=4.3,b=138").mobileAppVersion()).isEqualTo(Version(4, 3))
        assertThat(UserAgent("p=iOS,o=14.2,v=4.5,b=349").mobileAppVersion()).isEqualTo(Version(4, 5))
    }

    @Test
    fun `extracts app version containing major, minor and patch`() {
        assertThat(UserAgent("p=Android,o=29,v=4.3.0,b=138").mobileAppVersion()).isEqualTo(Version(4, 3, 0))
        assertThat(UserAgent("p=iOS,o=14.2,v=4.5.1,b=349").mobileAppVersion()).isEqualTo(Version(4, 5, 1))
    }

    @Test
    fun `handles invalid versions as unknown`() {
        assertThat(UserAgent("none").mobileAppVersion()).isEqualTo(Unknown)
        assertThat(UserAgent("").mobileAppVersion()).isEqualTo(Unknown)
        assertThat(UserAgent(".").mobileAppVersion()).isEqualTo(Unknown)
        assertThat(UserAgent("=").mobileAppVersion()).isEqualTo(Unknown)
        assertThat(UserAgent("p=Android,o=29,b=138").mobileAppVersion()).isEqualTo(Unknown)
        assertThat(UserAgent("p=Android,o=29,,b=138").mobileAppVersion()).isEqualTo(Unknown)
        assertThat(UserAgent("p=Android,o=29,v,b=138").mobileAppVersion()).isEqualTo(Unknown)
        assertThat(UserAgent("p=Android,o=29,v=,b=138").mobileAppVersion()).isEqualTo(Unknown)
        assertThat(UserAgent("p=Android,o=29,v==,b=138").mobileAppVersion()).isEqualTo(Unknown)
        assertThat(UserAgent("p=Android,o=29,v=.,b=138").mobileAppVersion()).isEqualTo(Unknown)
        assertThat(UserAgent("p=Android,o=29,v=4.,b=138").mobileAppVersion()).isEqualTo(Unknown)
        assertThat(UserAgent("p=Android,o=29,v=.4,b=138").mobileAppVersion()).isEqualTo(Unknown)
        assertThat(UserAgent("p=Android,o=29,v=4.0.,b=138").mobileAppVersion()).isEqualTo(Unknown)
        assertThat(UserAgent("p=Android,o=29,v=4.0.0.,b=138").mobileAppVersion()).isEqualTo(Unknown)
        assertThat(UserAgent("p=Android,o=29,v=4.0.0.0,b=138").mobileAppVersion()).isEqualTo(Unknown)
        assertThat(UserAgent("p=Android,o=29,v=4.a,b=138").mobileAppVersion()).isEqualTo(Unknown)
        assertThat(UserAgent("p=Android,o=29,v=4.0.0.a,b=138").mobileAppVersion()).isEqualTo(Unknown)
        assertThat(UserAgent("p=Android,o=29,v=a.4,b=138").mobileAppVersion()).isEqualTo(Unknown)
        assertThat(UserAgent("p=Android,o=29,v=a.b,b=138").mobileAppVersion()).isEqualTo(Unknown)
        assertThat(UserAgent("p=Android,o=29,v=a.b.c,b=138").mobileAppVersion()).isEqualTo(Unknown)
        assertThat(UserAgent("p=Android,o=29,v=abc,b=138").mobileAppVersion()).isEqualTo(Unknown)
        assertThat(UserAgent("p=Android,o=29,v=4=1.,b=138").mobileAppVersion()).isEqualTo(Unknown)
    }

}
