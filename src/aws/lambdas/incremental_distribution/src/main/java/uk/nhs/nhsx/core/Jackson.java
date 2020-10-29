package uk.nhs.nhsx.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class Jackson {

    private static final Logger logger = LogManager.getLogger(Jackson.class);

    private static <T> T readJson(String value, Class<T> clazz) throws JsonProcessingException {
        return SystemObjectMapper.MAPPER.readValue(value, clazz);
    }

    public static <T> T readJson(InputStream inputStream, Class<T> clazz) throws IOException {
        return SystemObjectMapper.MAPPER.readValue(inputStream, clazz);
    }

    public static <T> T readJson(InputStream inputStream, TypeReference<T> clazz) throws IOException {
        return SystemObjectMapper.MAPPER.readValue(inputStream, clazz);
    }

    public static <T> Optional<T> deserializeMaybe(String value, Class<T> toClass) {
        return deserializeMaybe(value, toClass, e -> logger.warn("Unable to deserialize payload", e));
    }

    public static <T> Optional<T> deserializeMaybeLogInfo(String value, Class<T> toClass) {
        return deserializeMaybe(value, toClass, e -> logger.info("Unable to deserialize payload", e));
    }

    public static <T> Optional<T> deserializeMaybe(String value, Class<T> toClass, Consumer<Exception> logMethod) {
        try {
            return Optional.ofNullable(readJson(value, toClass));
        } catch (Exception e) {
            logMethod.accept(e);
            return Optional.empty();
        }
    }

    public static <T> Optional<T> deserializeMaybeValidating(String value,
                                                             Class<T> toClass,
                                                             Function<T, T> customValidation) {
        try {
            return deserializeMaybe(value, toClass).map(customValidation);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static String toJson(Object value) {
        try {
            return SystemObjectMapper.MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
