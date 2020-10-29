package uk.nhs.nhsx.core;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class TestEnvironments {
    public static final Environment.Access NOTHING = new Environment.Access() {
        @Override
        public <T> Optional<T> optional(Environment.EnvironmentKey<T> key) {
            return Optional.empty();
        }
    };
    
    public static final Environment EMPTY = Environment.fromName("empty", NOTHING);

    public static final Function<Map<String, String>, Environment> TEST = t -> Environment.fromName("test", Environment.Access.TEST.apply(t));
}
