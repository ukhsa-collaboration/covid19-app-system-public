package uk.nhs.nhsx.core;

import java.time.Instant;
import java.util.function.Supplier;

import static java.time.Clock.systemUTC;

public interface SystemClock {
    Supplier<Instant> CLOCK = () -> systemUTC().instant();
}
