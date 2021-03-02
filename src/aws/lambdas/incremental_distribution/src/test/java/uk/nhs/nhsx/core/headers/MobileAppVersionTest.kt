package uk.nhs.nhsx.core.headers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.headers.MobileAppVersion.Version

class MobileAppVersionTest {

    @Test
    fun `compares major`() {
        assertThat(Version(1, 0) > Version(2, 0)).isFalse
        assertThat(Version(1, 0) >= Version(2, 0)).isFalse

        assertThat(Version(2, 0) > Version(1, 0)).isTrue
        assertThat(Version(2, 0) >= Version(1, 0)).isTrue

        assertThat(Version(1, 0) < Version(2, 0)).isTrue
        assertThat(Version(1, 0) <= Version(2, 0)).isTrue

        assertThat(Version(2, 0) < Version(1, 0)).isFalse
        assertThat(Version(2, 0) <= Version(1, 0)).isFalse

        assertThat(Version(1, 0) == Version(1, 0)).isTrue
        assertThat(Version(1, 0) == Version(2, 0)).isFalse
        assertThat(Version(2, 0) == Version(1, 0)).isFalse
    }

    @Test
    fun `compares major and minor`() {
        assertThat(Version(1, 0) > Version(1, 1)).isFalse
        assertThat(Version(1, 0) >= Version(1, 1)).isFalse

        assertThat(Version(1, 1) > Version(1, 0)).isTrue
        assertThat(Version(1, 1) >= Version(1, 0)).isTrue

        assertThat(Version(1, 0) < Version(1, 1)).isTrue
        assertThat(Version(1, 0) <= Version(1, 1)).isTrue

        assertThat(Version(1, 1) < Version(1, 0)).isFalse
        assertThat(Version(1, 1) <= Version(1, 0)).isFalse

        assertThat(Version(1, 1) == Version(1, 1)).isTrue
        assertThat(Version(2, 3) == Version(2, 4)).isFalse
        assertThat(Version(2, 4) == Version(2, 3)).isFalse
    }

    @Test
    fun `compares major, minor and patch`() {
        assertThat(Version(1, 0, 0) > Version(1, 0, 1)).isFalse
        assertThat(Version(1, 0, 0) >= Version(1, 0, 1)).isFalse

        assertThat(Version(1, 0, 1) > Version(1, 0, 0)).isTrue
        assertThat(Version(1, 0, 1) >= Version(1, 0, 0)).isTrue

        assertThat(Version(1, 0, 0) < Version(1, 0, 1)).isTrue
        assertThat(Version(1, 0, 0) <= Version(1, 0, 1)).isTrue

        assertThat(Version(1, 0, 1) < Version(1, 0, 0)).isFalse
        assertThat(Version(1, 0, 1) <= Version(1, 0, 0)).isFalse

        assertThat(Version(1, 0, 0) == Version(1, 0, 0)).isTrue
        assertThat(Version(1, 0, 1) == Version(1, 0, 0)).isFalse
        assertThat(Version(1, 0, 0) == Version(1, 0, 1)).isFalse
    }

    @Test
    fun `compares major, minor and patch using default patch value`() {
        assertThat(Version(1, 0) > Version(1, 0, 1)).isFalse
        assertThat(Version(1, 0) >= Version(1, 0, 1)).isFalse

        assertThat(Version(1, 0, 1) > Version(1, 0)).isTrue
        assertThat(Version(1, 0, 1) >= Version(1, 0)).isTrue

        assertThat(Version(1, 0) < Version(1, 0, 1)).isTrue
        assertThat(Version(1, 0) <= Version(1, 0, 1)).isTrue

        assertThat(Version(1, 0, 1) < Version(1, 0)).isFalse
        assertThat(Version(1, 0, 1) <= Version(1, 0)).isFalse

        assertThat(Version(1, 0) == Version(1, 0, 0)).isTrue
        assertThat(Version(1, 0, 0) == Version(1, 0)).isTrue
        assertThat(Version(1, 0, 1) == Version(1, 0)).isFalse
        assertThat(Version(1, 0) == Version(1, 0, 1)).isFalse
    }
}
