package uk.nhs.nhsx.core;

import uk.nhs.nhsx.core.Environment;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class TestEnvironments {
    public static final Environment EMPTY = Environment.fromName("empty", n -> Optional.empty());
    public static final Function<Map<String, String>, Environment> TEST = t -> Environment.fromName("test", n -> Optional.ofNullable(t.get(n)));
}
