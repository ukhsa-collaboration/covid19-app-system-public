package uk.nhs.nhsx.virology.policy

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.headers.MobileAppVersion

class MobileVersionCheckerTest {

    @Test
    fun `allows all versions`() {
        assertThat(AllVersions.invoke(MobileAppVersion.Version(1, 0))).isTrue
        assertThat(AllVersions.invoke(MobileAppVersion.Version(10, 1, 99))).isTrue
        assertThat(AllVersions.invoke(MobileAppVersion.Unknown)).isTrue
    }

    @Test
    fun `minimum inclusive version is required`() {
        assertThat(FromMinimumInclusive(MobileAppVersion.Version(1, 0)).invoke(MobileAppVersion.Version(2, 0))).isTrue
        assertThat(FromMinimumInclusive(MobileAppVersion.Version(1, 0)).invoke(MobileAppVersion.Version(1, 0))).isTrue
        assertThat(FromMinimumInclusive(MobileAppVersion.Version(2, 0)).invoke(MobileAppVersion.Version(1, 0))).isFalse
        assertThat(FromMinimumInclusive(MobileAppVersion.Version(2, 0)).invoke(MobileAppVersion.Unknown)).isFalse
    }
}
