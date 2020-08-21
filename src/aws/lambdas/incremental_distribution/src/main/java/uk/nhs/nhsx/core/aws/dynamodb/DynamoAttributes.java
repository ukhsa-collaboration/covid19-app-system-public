package uk.nhs.nhsx.core.aws.dynamodb;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;

import java.util.Collections;
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
            .flatMap(attributeValue -> Optional.ofNullable(attributeValue.getS()));
    }

    public static Map<String, AttributeValue> attributeMap(String key, String value) {
        return Collections.singletonMap(key, new AttributeValue(value));
    }

    public static Map<String, AttributeValue> attributeMap(String key, Number value) {
        return Collections.singletonMap(key, new AttributeValue().withN(String.valueOf(value)));
    }
}
