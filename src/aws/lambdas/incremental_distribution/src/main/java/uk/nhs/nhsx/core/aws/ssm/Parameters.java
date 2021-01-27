package uk.nhs.nhsx.core.aws.ssm;

import java.util.Set;
import java.util.function.Function;

public interface Parameters {

    Set<String> positive = Set.of("yes", "true", "enabled");

    <T> Parameter<T> parameter(ParameterName name, Function<String, T> convert);

    default <T extends Enum<T>> Parameter<T> ofEnum(ParameterName name, Class<T> type, T whenError) {
        return parameter(name, (v) -> convertEnum(type, v, whenError));
    }

    default <T extends Enum<T>> T convertEnum(Class<T> type, String value, T whenError) {
        try {
            return Enum.valueOf(type, value.toUpperCase());
        } catch (IllegalArgumentException noMatchingEnumValue) {
            return whenError;
        }
    }

    default Parameter<Boolean> ofBoolean(ParameterName name) {
        return parameter(name, (v) -> v!= null && positive.contains(v.toLowerCase()));
    }

    default Parameter<String> ofString(ParameterName name) {
        return parameter(name, Function.identity());
    }
}
