package uk.nhs.nhsx.isolationpayment

import org.junit.jupiter.api.Test
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.length
import strikt.assertions.matches
import strikt.assertions.message
import uk.nhs.nhsx.domain.IpcTokenId
import uk.nhs.nhsx.testhelper.data.constructWith

class IpcTokenIdGeneratorTest {

    @Test
    fun `test isolation token contains hexadecimals only`() {
        val token = IpcTokenIdGenerator.getToken()

        expectThat(token.value).matches(Regex("[A-Fa-f0-9]+"))
    }

    @Test
    fun `test isolation token length is sixty four characters`() {
        val token = IpcTokenIdGenerator.getToken()

        expectThat(token.value).length.isEqualTo(64)
    }

    @Test
    fun `fails validation`() {
        expectCatching { IpcTokenId.of("foobar") }
            .isFailure()
            .message
            .isA<String>()
            .contains("Validation failed for: (foobar)")
    }

    @Test
    fun `can create an invalid token using reflection`() {
        val ipcToken = constructWith<IpcTokenId>("invalid")

        expectThat(ipcToken.value).isEqualTo("invalid")
    }
}
