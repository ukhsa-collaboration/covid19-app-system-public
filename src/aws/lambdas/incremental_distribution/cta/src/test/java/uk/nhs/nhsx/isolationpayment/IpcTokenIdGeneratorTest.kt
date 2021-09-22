package uk.nhs.nhsx.isolationpayment

import org.junit.jupiter.api.Test
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.containsExactly
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.length
import strikt.assertions.matches
import strikt.assertions.message
import uk.nhs.nhsx.domain.IpcTokenId
import uk.nhs.nhsx.testhelper.data.constructWith
import java.util.*

class IpcTokenIdGeneratorTest {

    private val generator = RandomIpcTokenIdGenerator()

    @Test
    fun `isolation token contains hexadecimals only`() {
        expectThat(generator.nextToken())
            .get(IpcTokenId::value)
            .matches(Regex("[A-Fa-f0-9]+"))
    }

    @Test
    fun `isolation token length is sixty four characters`() {
        expectThat(generator.nextToken())
            .get(IpcTokenId::value)
            .length.isEqualTo(64)
    }

    @Test
    fun `isolation token has valid format`() {
        val generator = RandomIpcTokenIdGenerator(Random(127463))
        val tokens = (1..10).map { generator.nextToken() }.map(IpcTokenId::value)

        expectThat(tokens).containsExactly(
            "514d76db1722144a5945108ff76eb5710c40b649b94c146ad40b8af1bb5fa44d",
            "cb217bf057bb17e30f6b0efa942e2b0eb2644fb5bce7ccef0b344b78eb30159f",
            "7a4663c20a78c5d9dc625a55dc5e2e953f702b99ffc3089a9bf48f6818a4108d",
            "973fa9307aeaf265a0a87a2d0cdbfa8ae60cc6a63d3bb1e1f020938242d67905",
            "f002ec08f4f874eff415d8cf071c4e6eaf5b9c3776b7817c6c914d1d4e387f06",
            "66166cc44adacc43a2644056d4c8372ef57acd0e6cf73705e24d8e4684a7132d",
            "6ffe9d934f22c8f02981468e1cd73b46e95cd5f22663d80a1b88fd8c0f164253",
            "94fc2b475fde65a93aad6a0cad3b68c30eee5b0a6f70782f5566949880c31dc9",
            "f41d0177d4c192323d72340ef92a4067bb8b528c571741dbbe9d53fd4c055189",
            "bf99370192628eb5cd63a84997e99e179ccad960b1569b2cfddc42a3499f8c28"
        )
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
