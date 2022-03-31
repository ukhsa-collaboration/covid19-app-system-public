package uk.nhs.nhsx.core.aws.s3

import uk.nhs.nhsx.core.Clock
import uk.nhs.nhsx.core.UniqueId

class UniqueObjectKeyNameProvider(
    private val systemClock: Clock,
    private val uniqueId: UniqueId
) : ObjectKeyNameProvider {
    override fun generateObjectKeyName() = ObjectKey.of("${systemClock().toEpochMilli()}_${uniqueId()}")
}
