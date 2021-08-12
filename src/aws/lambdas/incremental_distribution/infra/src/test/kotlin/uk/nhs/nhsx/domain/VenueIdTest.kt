package uk.nhs.nhsx.domain

import org.junit.jupiter.api.Test
import strikt.api.expectCatching
import strikt.assertions.contains
import strikt.assertions.isFailure
import strikt.assertions.isNotNull
import strikt.assertions.isSuccess
import strikt.assertions.message

class VenueIdTest {

    @Test
    fun `creates a valid VenueId`() {
        expectCatching { (VenueId.of("CD3")) }.isSuccess()
    }

    @Test
    fun `throws validation exception if longer than 12 characters`() {
        expectCatching { VenueId.of("9".repeat(20)) }
            .isFailure()
            .message.isNotNull().contains("validation error: VenueId must match [CDEFHJKMPRTVWXY2345689]{1,12}")
    }

    @Test
    fun `throws validation exception if contains invalid characters`() {
        expectCatching { VenueId.of("L") }
            .isFailure()
            .message.isNotNull().contains("validation error: VenueId must match [CDEFHJKMPRTVWXY2345689]{1,12}")
    }
}
