package uk.nhs.nhsx.domain

import java.time.Duration

enum class TokenAgeRange() {
    LESS_THAN_24_HOURS, BETWEEN_24_48_HOURS, GREATER_THAN_48_HOURS;

    companion object {
        fun from(duration: Duration): TokenAgeRange =
            when {
                duration.toHours() < 24L -> {
                    LESS_THAN_24_HOURS
                }
                duration.toHours() < 48L -> {
                    BETWEEN_24_48_HOURS
                }
                else -> {
                    GREATER_THAN_48_HOURS
                }
            }
    }
}
