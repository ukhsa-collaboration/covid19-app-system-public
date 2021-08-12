package uk.nhs.nhsx.core.aws.s3

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.time.Instant
import java.util.*

class UniqueObjectKeyNameProviderTest {

    @Test
    fun `returns object key`() {
        val clock = { Instant.parse("2020-07-22T16:29:25.687835Z") }
        val uniqueId = { UUID.fromString("3ed625d7-8914-41be-b57b-60f1489f8e29") }
        val keyNameProvider = UniqueObjectKeyNameProvider(clock, uniqueId)

        expectThat(keyNameProvider.generateObjectKeyName())
            .isEqualTo(ObjectKey.of("1595435365687_3ed625d7-8914-41be-b57b-60f1489f8e29"))
    }
}
