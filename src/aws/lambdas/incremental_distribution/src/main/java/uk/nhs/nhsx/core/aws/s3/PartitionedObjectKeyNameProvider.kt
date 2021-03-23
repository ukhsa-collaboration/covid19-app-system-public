package uk.nhs.nhsx.core.aws.s3

import uk.nhs.nhsx.core.Clock
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.function.Supplier

class PartitionedObjectKeyNameProvider(
    private val systemClock: Clock,
    private val uniqueId: Supplier<UUID>
) : ObjectKeyNameProvider {

    override fun generateObjectKeyName(): ObjectKey {
        val now = systemClock()
        val prefix = DATE_TIME_FORMATTER.format(now.atZone(ZoneOffset.UTC))
        return ObjectKey.of(prefix + systemClock().toEpochMilli() + "_" + uniqueId.get().toString())
    }

    companion object {
        val DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd/HH/")
    }
}
