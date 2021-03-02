package uk.nhs.nhsx.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.function.Consumer;

public class Jackson {

    public static <T> T readJson(String value, Class<T> clazz) throws JsonProcessingException {
        return SystemObjectMapper.MAPPER.readValue(value, clazz);
    }

    public static <T> T readJson(InputStream inputStream, Class<T> clazz) throws IOException {
        return SystemObjectMapper.MAPPER.readValue(inputStream, clazz);
    }

    public static <T> T readJson(String value, TypeReference<T> clazz) throws IOException {
        return SystemObjectMapper.MAPPER.readValue(value, clazz);
    }

    public static <T> T readJson(InputStream inputStream, TypeReference<T> clazz) throws IOException {
        return SystemObjectMapper.MAPPER.readValue(inputStream, clazz);
    }

    public static <T> Optional<T> readMaybe(String value, Class<T> toClass, Consumer<Exception> handleError) {
        return Optional.ofNullable(readOrNull(value, toClass, handleError));
    }

    public static <T> T readOrNull(String value, Class<T> toClass, Consumer<Exception> handleError) {
        try {
            return readJson(value, toClass);
        } catch (Exception e) {
            handleError.accept(e);
            return null;
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
