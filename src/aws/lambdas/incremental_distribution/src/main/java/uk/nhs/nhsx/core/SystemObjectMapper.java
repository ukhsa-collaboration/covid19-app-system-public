package uk.nhs.nhsx.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

public class SystemObjectMapper {
    public static ObjectMapper MAPPER = objectMapper();

    private static ObjectMapper objectMapper() {
        return new ObjectMapper()
            .deactivateDefaultTyping()
            .registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES))
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, true)
            .configure(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES, true)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, true)
            .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, false)
            .configure(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY, true)
            .configure(DeserializationFeature.FAIL_ON_TRAILING_TOKENS, true)
            .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true)
            .configure(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS, false);
    }
}
