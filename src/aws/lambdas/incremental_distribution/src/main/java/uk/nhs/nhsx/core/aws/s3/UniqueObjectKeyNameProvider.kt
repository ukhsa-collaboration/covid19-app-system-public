package uk.nhs.nhsx.core.aws.s3

import java.time.Instant
import java.util.UUID
import java.util.function.Supplier

class UniqueObjectKeyNameProvider(private val systemClock: Supplier<Instant>, private val uniqueId: Supplier<UUID>) :
    ObjectKeyNameProvider {
    override fun generateObjectKeyName() =
        ObjectKey.of(systemClock.get().toEpochMilli().toString() + "_" + uniqueId.get().toString())
}
