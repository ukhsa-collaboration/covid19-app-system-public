package uk.nhs.nhsx.core.aws.dynamodb;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import dev.forkhandles.values.Value;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public class DynamoAttributes {

    public static String itemValueOrThrow(Map<String, AttributeValue> item, String key) {
        return Optional.ofNullable(item.get(key))
            .map(AttributeValue::getS)
            .orElseThrow(() -> new IllegalStateException("Required field missing"));
    }

    public static Optional<String> itemValueMaybe(Map<String, AttributeValue> item, String key) {
        return Optional.ofNullable(item.get(key))
            .map(AttributeValue::getS);
    }

    public static Optional<Integer> itemIntegerValueMaybe(Map<String, AttributeValue> item, String key) {
        return Optional.ofNullable(item.get(key))
            .map(AttributeValue::getN)
            .map(Integer::parseInt);
    }

    public static Optional<Long> itemLongValueMaybe(Map<String, AttributeValue> item, String key) {
        return Optional.ofNullable(item.get(key))
            .map(AttributeValue::getN)
            .map(Long::valueOf);
    }

    public static AttributeValue stringAttribute(Instant instant) {
        return DynamoAttributes.stringAttribute(instant.toString());
    }

    public static Map<String, AttributeValue> attributeMap(String key, String value) {
        return Map.of(key, stringAttribute(value));
    }

    public static Map<String, AttributeValue> attributeMap(String key, Number value) {
        return Map.of(key, numericAttribute(value));
    }

    public static Map<String, AttributeValue> attributeMap(String key, Value<String> value) {
        return Map.of(key, stringAttribute(value));
    }

    public static AttributeValue stringAttribute(String value) {
        return new AttributeValue().withS(value);
    }

    public static AttributeValue stringAttribute(Value<String> value) {
        return new AttributeValue().withS(value.getValue());
    }

    public static AttributeValue numericAttribute(Number value) {
        return new AttributeValue().withN(String.valueOf(value));
    }

    public static AttributeValue numericAttribute(Instant value) {
        return new AttributeValue().withN(String.valueOf(value.getEpochSecond()));
    }

    public static AttributeValue numericNullableAttribute(Number value) {
        return value == null
            ? new AttributeValue().withNULL(Boolean.TRUE)
            : new AttributeValue().withN(String.valueOf(value));
    }
}
