package uk.nhs.nhsx.core

import com.amazonaws.services.kms.AWSKMS
import com.amazonaws.services.kms.model.SignResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.aws.ssm.Parameter
import uk.nhs.nhsx.core.aws.ssm.ParameterName
import uk.nhs.nhsx.core.aws.ssm.Parameters
import uk.nhs.nhsx.core.signature.KeyId
import java.nio.ByteBuffer

internal class StandardSigningFactoryTest {

    @Test
    fun `SSM is only called once and result cached`() {
        val parameters = mockk<Parameters>()
        val name = ParameterName.of("foobar")

        every { parameters.parameter<KeyId>(name, any()) } returns Parameter { KeyId.of("foobarKey") }

        val client: AWSKMS = mockk()
        every { client.sign(any()) } returns SignResult().withSignature(ByteBuffer.allocate(0))

        val signer = StandardSigningFactory(
            SystemClock.CLOCK,
            parameters,
            client
        ).signContentWithKeyFromParameter(name)

        signer.sign(ByteArray(0))
        signer.sign(ByteArray(0))

        verify(exactly = 1) { parameters.parameter<KeyId>(name, any()) }
        verify(exactly = 2) { client.sign(any()) }
    }
}
