package uk.nhs.nhsx.core.aws.secretsmanager

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.util.*

class CachingSecretManagerTest {

    @Test
    fun `caches secret value`() {
        val secretName = SecretName.of("Foobar")

        val delegate = mockk<SecretManager>()
        every { delegate.getSecret(secretName) } returns Optional.ofNullable(SecretValue.of("Fooabr"))

        val manager = CachingSecretManager(delegate)

        repeat(3) { manager.getSecret(secretName) }

        verify(exactly = 1) { delegate.getSecret(secretName) }
    }

    @Test
    fun `caches empty secret value`() {
        val secretName = SecretName.of("Foobar")

        val delegate = mockk<SecretManager>()
        every { delegate.getSecret(secretName) } returns Optional.empty()

        val manager = CachingSecretManager(delegate)

        repeat(3) { manager.getSecret(secretName) }

        verify(exactly = 1) { delegate.getSecret(secretName) }
    }
}
