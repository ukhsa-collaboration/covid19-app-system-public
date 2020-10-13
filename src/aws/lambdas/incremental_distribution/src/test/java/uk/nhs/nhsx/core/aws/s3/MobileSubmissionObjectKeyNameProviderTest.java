package uk.nhs.nhsx.core.aws.s3;

import org.junit.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

public class MobileSubmissionObjectKeyNameProviderTest {

    @Test
    public void returnsObjectKey() {
        Supplier<Instant> systemClock = () -> LocalDateTime
            .parse("2020-07-22T16:29:25.687835")
            .toInstant(ZoneOffset.UTC);

        Supplier<UUID> uniqueId = () -> UUID
            .fromString("3ed625d7-8914-41be-b57b-60f1489f8e29");

        MobileSubmissionObjectKeyNameProvider objectKeyNameProvider = new MobileSubmissionObjectKeyNameProvider(systemClock, uniqueId);
        assertThat(objectKeyNameProvider.generateObjectKeyName())
            .isEqualTo(ObjectKey.of("mobile/20200722/3ed625d7-8914-41be-b57b-60f1489f8e29"));

    }
}
