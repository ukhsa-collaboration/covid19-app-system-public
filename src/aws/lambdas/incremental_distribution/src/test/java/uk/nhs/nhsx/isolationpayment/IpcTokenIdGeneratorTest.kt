package uk.nhs.nhsx.isolationpayment

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.testhelper.data.constructWith
import uk.nhs.nhsx.virology.IpcTokenId

class IpcTokenIdGeneratorTest {

    @Test
    fun `test isolation token contains hexadecimals only`() {
        val token = IpcTokenIdGenerator.getToken()
        assertThat(token.value).matches("[A-Fa-f0-9]+")
    }

    @Test
    fun `test isolation token length is sixty four characters`() {
        val token = IpcTokenIdGenerator.getToken()
        assertThat(token.value).hasSize(64)
    }

    @Test
    fun `fails validation`() {
        assertThatThrownBy { IpcTokenId.of("foobar") }.hasMessage("Validation failed for: (foobar)")
    }

    @Test
    fun `can create an invalid token using reflection`() {
        val ipcToken = constructWith<IpcTokenId>("invalid")
        assertThat(ipcToken.value).isEqualTo("invalid")
    }
}
