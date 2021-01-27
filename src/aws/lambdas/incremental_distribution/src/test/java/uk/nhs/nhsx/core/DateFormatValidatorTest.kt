package uk.nhs.nhsx.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.*

class DateFormatValidatorTest {

    @Test
    fun checkValidDate() {
        assertThat(DateFormatValidator.isValid("2019-07-04T23:33:03Z")).isTrue
        assertThat(DateFormatValidator.toZonedDateTimeMaybe("2019-07-04T23:33:03Z"))
            .isEqualTo(Optional.of(ZonedDateTime.parse("2019-07-04T23:33:03Z")))
    }

    @Test
    fun checkInValidDate() {
        assertThat(DateFormatValidator.isValid("2019-07-04FT23:33:03Z")).isFalse
        assertThat(DateFormatValidator.toZonedDateTimeMaybe("2019-07-04FT23:33:03Z"))
            .isEqualTo(Optional.empty<Any>())
    }
}