package uk.nhs.nhsx.core.aws.s3

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.function.Supplier

class PartitionedObjectKeyNameProvider(
    private val systemClock: Supplier<Instant>,
    private val uniqueId: Supplier<UUID>
) : ObjectKeyNameProvider {

    override fun generateObjectKeyName(): ObjectKey {
        val now = systemClock.get()
        val prefix = DATE_TIME_FORMATTER.format(now.atZone(ZoneOffset.UTC))
        return ObjectKey.of(prefix + systemClock.get().toEpochMilli() + "_" + uniqueId.get().toString())
    }

    companion object {
        val DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd/HH/")
    }
}
