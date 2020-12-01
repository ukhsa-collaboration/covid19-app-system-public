package uk.nhs.nhsx.core.aws.s3;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.function.Supplier;

public class PartitionedObjectKeyNameProvider implements ObjectKeyNameProvider {

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd/HH/");
    private final Supplier<Instant> systemClock;
    private final Supplier<UUID> uniqueId;

    public PartitionedObjectKeyNameProvider(Supplier<Instant> systemClock, Supplier<UUID> uniqueId) {
        this.systemClock = systemClock;
        this.uniqueId = uniqueId;
    }

    @Override
    public ObjectKey generateObjectKeyName() {
        Instant now = systemClock.get();
        String prefix = DATE_TIME_FORMATTER.format(now.atZone(ZoneOffset.UTC));
        return ObjectKey.of(prefix + systemClock.get().toEpochMilli() + "_" + uniqueId.get().toString());
    }
}
