package uk.nhs.nhsx.core.aws.dynamodb

import com.amazonaws.services.dynamodbv2.model.AttributeValue
import dev.forkhandles.values.Value
import java.time.Instant

object DynamoAttributes {
    fun itemValueOrThrow(item: Map<String, AttributeValue>, key: String): String = item[key]?.s
        ?: throw IllegalStateException("Required field missing")

    fun itemIntegerValueOrNull(item: Map<String, AttributeValue>, key: String) = item[key]?.n?.toIntOrNull()

    fun itemLongValueOrNull(item: Map<String, AttributeValue>, key: String): Long? = item[key]?.n?.toLongOrNull()
    fun itemLongValueOrThrow(item: Map<String, AttributeValue>, key: String): Long = item[key]?.n?.toLongOrNull()
        ?: throw IllegalStateException("Required field missing")

    fun itemValueOrNull(item: Map<String, AttributeValue>, key: String) = item[key]?.s

    fun attributeMap(key: String, value: String): Map<String, AttributeValue> =
        mapOf(key to stringAttribute(value))

    fun attributeMap(key: String, value: Number): Map<String, AttributeValue> =
        mapOf(key to numericAttribute(value))

    fun attributeMap(key: String, value: Value<String>): Map<String, AttributeValue> =
        mapOf(key to stringAttribute(value))

    fun stringAttribute(value: String): AttributeValue = AttributeValue().withS(value)

    fun stringAttribute(value: Value<String>): AttributeValue = AttributeValue().withS(value.value)

    fun numericAttribute(value: Number): AttributeValue = AttributeValue().withN(value.toString())

    fun numericAttribute(value: Instant): AttributeValue = AttributeValue().withN(value.epochSecond.toString())

    fun numericNullableAttribute(value: Number?): AttributeValue =
        if (value == null) AttributeValue().withNULL(true) else AttributeValue().withN(value.toString())
}
