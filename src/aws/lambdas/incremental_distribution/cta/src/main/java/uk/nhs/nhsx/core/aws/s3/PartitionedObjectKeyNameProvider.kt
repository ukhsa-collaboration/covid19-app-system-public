package uk.nhs.nhsx.core.aws.s3

import uk.nhs.nhsx.core.Clock
import uk.nhs.nhsx.core.UniqueId
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatter

class PartitionedObjectKeyNameProvider(
    private val systemClock: Clock,
    private val uniqueId: UniqueId
) : ObjectKeyNameProvider {

    override fun generateObjectKeyName(): ObjectKey {
        val now = systemClock()
        val prefix = DATE_TIME_FORMATTER.format(now.atZone(UTC))
        return ObjectKey.of("""$prefix${systemClock().toEpochMilli()}_${uniqueId()}""")
    }

    companion object {
        val DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd/HH/")
    }
}
