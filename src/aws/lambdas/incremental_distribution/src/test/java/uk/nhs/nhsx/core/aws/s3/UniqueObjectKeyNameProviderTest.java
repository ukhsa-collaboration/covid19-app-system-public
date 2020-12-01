package uk.nhs.nhsx.core.aws.s3;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

public class UniqueObjectKeyNameProviderTest {

    @Test
    public void returnsObjectKey() {
        Supplier<Instant> systemClock = () -> LocalDateTime
            .parse("2020-07-22T16:29:25.687835")
            .toInstant(ZoneOffset.UTC);

        Supplier<UUID> uniqueId = () -> UUID
            .fromString("3ed625d7-8914-41be-b57b-60f1489f8e29");

        UniqueObjectKeyNameProvider uniqueObjectKeyNameProvider = new UniqueObjectKeyNameProvider(systemClock, uniqueId);
        assertThat(uniqueObjectKeyNameProvider.generateObjectKeyName())
            .isEqualTo(ObjectKey.of("1595435365687_3ed625d7-8914-41be-b57b-60f1489f8e29"));

    }


}