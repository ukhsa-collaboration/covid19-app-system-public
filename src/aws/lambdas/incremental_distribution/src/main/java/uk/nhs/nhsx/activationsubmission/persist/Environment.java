package uk.nhs.nhsx.activationsubmission.persist;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.nhs.nhsx.core.ValueType;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class Environment {

    private static final String UNKNOWN = "unknown";
    private final Logger logger = LogManager.getLogger(Environment.class);

    public enum EnvironmentType {NonProduction, Production;}

    public static class EnvironmentName extends ValueType<EnvironmentName> {

        protected EnvironmentName(String value) {
            super(value);
        }

        public static EnvironmentName of(String value) {
            return new EnvironmentName(value);
        }

    }

    public final EnvironmentName name;

    public final EnvironmentType type;
    public final Access access;

    private Environment(EnvironmentName name, EnvironmentType type, Access access) {
        this.name = name;
        this.type = type;
        this.access = access;
    }

    public <T> Optional<T> whenNonProduction(String functionality, Function<Environment, T> consumer) {
        if (type == EnvironmentType.NonProduction) {
            logger.error("ENABLING TEST FUNCTIONALITY:" + functionality);
            return Optional.ofNullable(consumer.apply(this));
        }
        return Optional.empty();
    }

    public static Environment unknown() {
        return fromName(UNKNOWN, Access.SYSTEM);
    }

    public static Environment fromSystem() {
        return fromEnvironment(Access.SYSTEM);
    }

    public static Environment fromEnvironment(Environment.Access access) {
        return fromName(access.required("WORKSPACE"), access);
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

    public interface Access {
        Access SYSTEM = name -> Optional.ofNullable(System.getenv(name));
        Function<Map<String, String>, Access> TEST = (m) -> (n) -> Optional.ofNullable(m.get(n));

        Optional<String> optional(String name);

        default String required(String name) {
            return optional(name).orElseThrow(() -> new IllegalArgumentException(String.format("Environment variable %s is missing", name)));
        }
    }
}
