package uk.nhs.nhsx.virology.policy

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import uk.nhs.nhsx.core.headers.MobileAppVersion

class MobileVersionCheckerTest {

    @Test
    fun `allows all versions`() {
        expectThat(AllVersions.invoke(MobileAppVersion.Version(1, 0))).isTrue()
        expectThat(AllVersions.invoke(MobileAppVersion.Version(10, 1, 99))).isTrue()
        expectThat(AllVersions.invoke(MobileAppVersion.Unknown)).isTrue()
    }

    @Test
    fun `minimum inclusive version is required`() {
        expectThat(FromMinimumInclusive(MobileAppVersion.Version(1, 0)).invoke(MobileAppVersion.Version(2, 0))).isTrue()
        expectThat(FromMinimumInclusive(MobileAppVersion.Version(1, 0)).invoke(MobileAppVersion.Version(1, 0))).isTrue()
        expectThat(FromMinimumInclusive(MobileAppVersion.Version(2, 0)).invoke(MobileAppVersion.Version(1, 0))).isFalse()
        expectThat(FromMinimumInclusive(MobileAppVersion.Version(2, 0)).invoke(MobileAppVersion.Unknown)).isFalse()
    }
}
