package uk.nhs.nhsx.core.headers

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThan
import strikt.assertions.isGreaterThanOrEqualTo
import strikt.assertions.isLessThan
import strikt.assertions.isLessThanOrEqualTo
import uk.nhs.nhsx.core.headers.MobileAppVersion.Version

class MobileAppVersionTest {

    private fun v(version: String) = version.split(".").let {
        Version(it[0].toInt(), it[1].toInt(), it.getOrElse(2) { "0" }.toInt())
    }

    @Test
    fun `compares major`() {
        expectThat(v("1.0")).not().isGreaterThan(v("2.0"))
        expectThat(v("1.0")).not().isGreaterThanOrEqualTo(v("2.0"))

        expectThat(v("2.0")).isGreaterThan(v("1.0"))
        expectThat(v("2.0")).isGreaterThanOrEqualTo(v("1.0"))

        expectThat(v("1.0")).isLessThan(v("2.0"))
        expectThat(v("1.0")).isLessThanOrEqualTo(v("2.0"))

        expectThat(v("2.0")).not().isLessThan(v("1.0"))
        expectThat(v("2.0")).not().isLessThanOrEqualTo(v("1.0"))

        expectThat(v("1.0")).isEqualTo(v("1.0"))
        expectThat(v("1.0")).not().isEqualTo(v("2.0"))
        expectThat(v("2.0")).not().isEqualTo(v("1.0"))
    }

    @Test
    fun `compares major and minor`() {
        expectThat(v("1.0")).not().isGreaterThan(v("1.1"))
        expectThat(v("1.0")).not().isGreaterThanOrEqualTo(v("1.1"))

        expectThat(v("1.1")).isGreaterThan(v("1.0"))
        expectThat(v("1.1")).isGreaterThanOrEqualTo(v("1.0"))

        expectThat(v("1.0")).isLessThan(v("1.1"))
        expectThat(v("1.0")).isLessThanOrEqualTo(v("1.1"))

        expectThat(v("1.1")).not().isLessThan(v("1.0"))
        expectThat(v("1.1")).not().isLessThanOrEqualTo(v("1.0"))

        expectThat(v("1.1")).isEqualTo(v("1.1"))
        expectThat(v("2.3")).not().isEqualTo(v("2.4"))
        expectThat(v("2.4")).not().isEqualTo(v("2.3"))
    }

    @Test
    fun `compares major, minor and patch`() {
        expectThat(v("1.0.0")).not().isGreaterThan(v("1.0.1"))
        expectThat(v("1.0.0")).not().isGreaterThanOrEqualTo(v("1.0.1"))

        expectThat(v("1.0.1")).isGreaterThan(v("1.0.0"))
        expectThat(v("1.0.1")).isGreaterThanOrEqualTo(v("1.0.0"))

        expectThat(v("1.0.0")).isLessThan(v("1.0.1"))
        expectThat(v("1.0.0")).isLessThanOrEqualTo(v("1.0.1"))

        expectThat(v("1.0.1")).not().isLessThan(v("1.0.0"))
        expectThat(v("1.0.1")).not().isLessThanOrEqualTo(v("1.0.0"))

        expectThat(v("1.0.0")).isEqualTo(v("1.0.0"))
        expectThat(v("1.0.1")).not().isEqualTo(v("1.0.0"))
        expectThat(v("1.0.0")).not().isEqualTo(v("1.0.1"))
    }

    @Test
    fun `compares major, minor and patch using default patch value`() {
        expectThat(v("1.0")).not().isGreaterThan(v("1.0.1"))
        expectThat(v("1.0")).not().isGreaterThanOrEqualTo(v("1.0.1"))

        expectThat(v("1.0.1")).isGreaterThan(v("1.0"))
        expectThat(v("1.0.1")).isGreaterThanOrEqualTo(v("1.0"))

        expectThat(v("1.0")).isLessThan(v("1.0.1"))
        expectThat(v("1.0")).isLessThanOrEqualTo(v("1.0.1"))

        expectThat(v("1.0.1")).not().isLessThan(v("1.0"))
        expectThat(v("1.0.1")).not().isLessThanOrEqualTo(v("1.0"))

        expectThat(v("1.0")).isEqualTo(v("1.0.0"))
        expectThat(v("1.0.0")).isEqualTo(v("1.0"))
        expectThat(v("1.0.1")).not().isEqualTo(v("1.0"))
        expectThat(v("1.0")).not().isEqualTo(v("1.0.1"))
    }
}
