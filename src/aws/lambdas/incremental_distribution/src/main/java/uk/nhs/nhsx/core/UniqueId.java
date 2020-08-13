package uk.nhs.nhsx.core;

import java.util.UUID;
import java.util.function.Supplier;

public interface UniqueId {
    Supplier<UUID> ID = UUID::randomUUID;
}
