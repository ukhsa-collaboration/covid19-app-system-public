package uk.nhs.nhsx.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.nhs.nhsx.core.exceptions.Defect;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

public class Environment {

    public static final EnvironmentKey<String> WORKSPACE = EnvironmentKey.string("WORKSPACE");

    private static final String UNKNOWN = "unknown";
    public final EnvironmentName name;

    public final EnvironmentType type;
    public final Access access;
    private final Logger logger = LogManager.getLogger(Environment.class);

    private Environment(EnvironmentName name, EnvironmentType type, Access access) {
        this.name = name;
        this.type = type;
        this.access = access;
    }

    public static Environment unknown() {
        return fromName(UNKNOWN, Access.SYSTEM);
    }

    public static Environment fromSystem() {
        return fromEnvironment(Access.SYSTEM);
    }

    public static Environment fromEnvironment(Environment.Access access) {
        return fromName(access.required(WORKSPACE), access);
    }

    public static Environment fromName(String name, Environment.Access access) {
        return new Environment(
            EnvironmentName.of(name), environmentTypeFrom(name), access
        );
    }

    private static EnvironmentType environmentTypeFrom(String name) {
        if (name == null || name.equals(UNKNOWN) || name.startsWith("te-prod")) {
            return EnvironmentType.Production;
        }
        return EnvironmentType.NonProduction;
    }

    public <T> Optional<T> whenNonProduction(String functionality, Function<Environment, T> consumer) {
        if (type == EnvironmentType.NonProduction) {
            logger.error("ENABLING TEST FUNCTIONALITY:" + functionality);
            return Optional.ofNullable(consumer.apply(this));
        }
        return Optional.empty();
    }

    public enum EnvironmentType {NonProduction, Production}

    public interface Access {
        Access SYSTEM = new Access() {
            @Override
            public <T> Optional<T> optional(EnvironmentKey<T> key) {
                return Optional.ofNullable(System.getenv(key.name)).map(key.conversion);
            }
        };

        Function<Map<String, String>, Access> TEST = m -> new Access() {
            @Override
            public <T> Optional<T> optional(EnvironmentKey<T> key) {
                return Optional.ofNullable(m.get(key.name)).map(key.conversion);
            }
        };

        <T> Optional<T> optional(EnvironmentKey<T> key);

        default <T> T required(EnvironmentKey<T> key) {
            return optional(key).orElseThrow(() -> new IllegalArgumentException(String.format("Environment variable %s is missing", key.name)));
        }
    }

    public static class EnvironmentName extends ValueType<EnvironmentName> {

        protected EnvironmentName(String value) {
            super(value);
        }

        public static EnvironmentName of(String value) {
            return new EnvironmentName(value);
        }
    }

    public static class EnvironmentKey<T> {
        public final String name;
        public final Function<String, T> conversion;

        public EnvironmentKey(String name, Function<String, T> conversion) {
            this.name = name;
            this.conversion = conversion;
        }

        public static <T> EnvironmentKey<T> define(String name, Function<String, T> conversion) {
            return new EnvironmentKey<>(name, conversion);
        }

        public static EnvironmentKey<String> string(String name) {
            return define(name, identity());
        }

        public static EnvironmentKey<Boolean> bool(String name) {
            return define(name, Boolean::parseBoolean);
        }

        public static EnvironmentKey<Integer> integer(String name) {
            return define(name, Integer::parseInt);
        }

        public static <T> EnvironmentKey<List<T>> list(String name, Function<String, T> conversion) {
            return define(name, s -> Arrays.stream(s.split(",")).filter(it -> !it.isBlank()).map(conversion).collect(Collectors.toList()));
        }

        public static EnvironmentKey<List<String>> strings(String name) {
            return list(name, identity());
        }

        public static <T extends ValueType<T>> EnvironmentKey<T> value(String name, Class<T> clazz) {
            return define(name, valueTypeConverterFor(clazz));
        }

        private static <T extends ValueType<T>> Function<String, T> valueTypeConverterFor(Class<T> clazz) {
            return s -> {
                try {
                    return clazz.cast(clazz.getMethod("of", String.class).invoke(null, s));
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    throw new Defect("ValueType missing expected method String of(String)", e);
                }
            };
        }
    }
}
