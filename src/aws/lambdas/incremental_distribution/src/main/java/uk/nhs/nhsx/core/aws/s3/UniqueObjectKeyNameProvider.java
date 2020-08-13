package uk.nhs.nhsx.core.aws.s3;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;

public class UniqueObjectKeyNameProvider implements ObjectKeyNameProvider {

    private final Supplier<Instant> systemClock;
    private final Supplier<UUID> uniqueId;

    public UniqueObjectKeyNameProvider(Supplier<Instant> systemClock, Supplier<UUID> uniqueId) {
        this.systemClock = systemClock;
        this.uniqueId = uniqueId;
    }

    @Override
    public ObjectKey generateObjectKeyName() {
        return ObjectKey.of(systemClock.get().toEpochMilli() + "_" + uniqueId.get().toString());
    }
}
