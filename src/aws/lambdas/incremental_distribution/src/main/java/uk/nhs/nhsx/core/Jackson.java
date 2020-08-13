package uk.nhs.nhsx.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public class Jackson {

    private static final Logger logger = LoggerFactory.getLogger(Jackson.class);

    private static <T> T readJson(String value, Class<T> clazz) throws JsonProcessingException {
        return SystemObjectMapper.MAPPER.readValue(value, clazz);
    }

    public static <T> T readJson(InputStream inputStream, Class<T> clazz) throws IOException {
        return SystemObjectMapper.MAPPER.readValue(inputStream, clazz);
    }

    public static <T> Optional<T> deserializeMaybe(String value, Class<T> toClass) {
        try {
            return Optional.ofNullable(readJson(value, toClass));
        } catch (Exception e) {
            logger.warn("Unable to deserialize payload", e);
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
