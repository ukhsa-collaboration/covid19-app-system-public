package uk.nhs.nhsx.core.aws.dynamodb;

import com.amazonaws.services.dynamodbv2.model.AttributeAction;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.ExpectedAttributeValue;

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
    public static Optional<Integer> itemIntegerValueMaybe(Map<String, AttributeValue> item, String key) {
        return Optional.ofNullable(item.get(key))
            .flatMap(attributeValue -> Optional.of(Integer.parseInt(attributeValue.getN())));
    }

    public static Optional<Long> itemLongValueMaybe(Map<String, AttributeValue> item, String key) {
        return Optional.ofNullable(item.get(key)).map(AttributeValue::getN)
            .map(Long::valueOf);
    }

    public static Map<String, AttributeValue> attributeMap(String key, String value) {
        return Collections.singletonMap(key, stringAttribute(value));
    }

    public static Map<String, AttributeValue> attributeMap(String key, Number value) {
        return Collections.singletonMap(key, numericAttribute(value));
    }

    public static AttributeValue stringAttribute(String value) {
        return new AttributeValue().withS(value);
    }
    
    public static AttributeValue numericAttribute(Number value) {
        return new AttributeValue().withN(String.valueOf(value));
    }

    public static AttributeValue numericNullableAttribute(Number value) {
        return value == null
            ? new AttributeValue().withNULL(Boolean.TRUE)
            : new AttributeValue().withN(String.valueOf(value));
    }

    public static AttributeValueUpdate attributeValueUpdate(AttributeValue value, AttributeAction action) {
        return new AttributeValueUpdate().withValue(value).withAction(action);
    }

    public static ExpectedAttributeValue expectedAttributeExists(AttributeValue value) {
        return new ExpectedAttributeValue(value);
    }

    public static ExpectedAttributeValue expectedAttributeDoesNotExist() {
        return new ExpectedAttributeValue(false);
    }
}
