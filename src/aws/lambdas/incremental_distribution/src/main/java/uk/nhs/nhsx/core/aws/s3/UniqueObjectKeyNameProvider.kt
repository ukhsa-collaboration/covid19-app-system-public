package uk.nhs.nhsx.core.aws.s3

import uk.nhs.nhsx.core.Clock
import java.util.UUID
import java.util.function.Supplier

class UniqueObjectKeyNameProvider(private val systemClock: Clock, private val uniqueId: Supplier<UUID>) :
    ObjectKeyNameProvider {
    override fun generateObjectKeyName() =
        ObjectKey.of(systemClock().toEpochMilli().toString() + "_" + uniqueId.get().toString())
}
