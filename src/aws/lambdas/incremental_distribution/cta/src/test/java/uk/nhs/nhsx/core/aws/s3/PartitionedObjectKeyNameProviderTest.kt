package uk.nhs.nhsx.core.aws.s3

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*

class PartitionedObjectKeyNameProviderTest {

    @Test
    fun `generate object key name at zero time`() {
        val uuid = UUID.randomUUID()
        val objectKeyNameProvider = PartitionedObjectKeyNameProvider({ Instant.ofEpochSecond(0) }, { uuid })
        val objectKeyName = objectKeyNameProvider.generateObjectKeyName()

        expectThat(objectKeyName.value).isEqualTo("1970/01/01/00/0_${uuid}")
    }

    @Test
    fun `generate object key name at non-zero time`() {
        val dateTime = ZonedDateTime.of(2020, 11, 12, 13, 7, 9, 0, ZoneOffset.UTC)
        val uuid = UUID.randomUUID()
        val objectKeyNameProvider = PartitionedObjectKeyNameProvider({ dateTime.toInstant() }, { uuid })
        val objectKeyName = objectKeyNameProvider.generateObjectKeyName()

        expectThat(objectKeyName.value).isEqualTo("2020/11/12/13/1605186429000_${uuid}")
    }

}
