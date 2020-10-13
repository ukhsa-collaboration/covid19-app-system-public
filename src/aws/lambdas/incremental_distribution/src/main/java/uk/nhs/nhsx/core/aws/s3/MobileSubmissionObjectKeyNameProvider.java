package uk.nhs.nhsx.core.aws.s3;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.function.Supplier;

public class MobileSubmissionObjectKeyNameProvider implements ObjectKeyNameProvider{
    private final Supplier<Instant> systemClock;
    private final Supplier<UUID> uniqueId;

    public MobileSubmissionObjectKeyNameProvider(Supplier<Instant> systemClock, Supplier<UUID> uniqueId) {
        this.systemClock = systemClock;
        this.uniqueId = uniqueId;
    }

    @Override
    public ObjectKey generateObjectKeyName() {
        return ObjectKey.of("mobile/" + getDate() + "/" + uniqueId.get().toString());
    }

    private String getDate(){
        Instant instant = systemClock.get();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);
        return formatter.format(instant);
    }
}
