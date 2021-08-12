package uk.nhs.nhsx.domain

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.message

class CtaTokenTest {

    @Test
    fun `create valid ctaToken`() {
        expectThat(CtaToken.of("cc8f0b6z").value).isEqualTo("cc8f0b6z")
    }

    @Test
    fun `throw exception when invalid ctaToken`() {
        expectThrows<IllegalArgumentException> { CtaToken.of("invalid-token") }
            .message.isNotNull().contains("Validation failed for: (invalid-token)")
    }
}
