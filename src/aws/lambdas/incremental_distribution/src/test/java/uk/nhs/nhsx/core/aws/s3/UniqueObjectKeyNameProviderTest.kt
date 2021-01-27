package uk.nhs.nhsx.core.aws.s3

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import java.util.function.Supplier

class UniqueObjectKeyNameProviderTest {

    @Test
    fun returnsObjectKey() {
        val systemClock = Supplier {
            LocalDateTime
                .parse("2020-07-22T16:29:25.687835")
                .toInstant(ZoneOffset.UTC)
        }

        val uniqueId = Supplier {
            UUID.fromString("3ed625d7-8914-41be-b57b-60f1489f8e29")
        }

        val uniqueObjectKeyNameProvider = UniqueObjectKeyNameProvider(systemClock, uniqueId)
        assertThat(uniqueObjectKeyNameProvider.generateObjectKeyName())
            .isEqualTo(ObjectKey.of("1595435365687_3ed625d7-8914-41be-b57b-60f1489f8e29"))
    }
}