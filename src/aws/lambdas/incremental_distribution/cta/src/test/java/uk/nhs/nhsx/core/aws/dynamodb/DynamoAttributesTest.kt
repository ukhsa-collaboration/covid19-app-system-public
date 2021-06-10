package uk.nhs.nhsx.core.aws.dynamodb

import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.Collections.singletonMap
import java.util.Optional

class DynamoAttributesTest {

    @Test
    fun `item value required for existing item key`() {
        val item = mapOf("key" to AttributeValue("value"))
        val value = DynamoAttributes.itemValueOrThrow(item, "key")
        assertThat(value, equalTo("value"))
    }

    @Test
    fun `item value required for non existing item key`() {
        val item = mapOf("key" to AttributeValue("value"))
        assertThatThrownBy { DynamoAttributes.itemValueOrThrow(item, "key-123") }
            .isExactlyInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `item value required for existing item key but with different type`() {
        val item = mapOf("key" to AttributeValue().withN("1"))
        assertThatThrownBy { DynamoAttributes.itemValueOrThrow(item, "key") }
            .isExactlyInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `item string maybe for existing item key`() {
        val item = mapOf("key" to AttributeValue("value"))
        val value = DynamoAttributes.itemValueOrNull(item, "key")
        assertThat(value, equalTo("value"))
    }

    @Test
    fun `item string maybe for non existing item key`() {
        val item = mapOf("key" to AttributeValue("value"))
        val value = DynamoAttributes.itemValueOrNull(item, "key-123")
        assertThat(value, absent())
    }

    @Test
    fun `item string maybe for existing item key but with different type`() {
        val item = mapOf("key" to AttributeValue().withN("1"))
        val value = DynamoAttributes.itemValueOrNull(item, "key")
        assertThat(value, absent())
    }

    @Test
    fun `creates singleton map with string attribute value`() {
        val attributeMap = DynamoAttributes.attributeMap("key", "value")
        assertThat(attributeMap, equalTo(singletonMap("key", AttributeValue("value"))))
    }

    @Test
    fun `creates singleton map with number attribute value`() {
        val attributeMap = DynamoAttributes.attributeMap("key", 1)
        assertThat(attributeMap, equalTo(singletonMap("key", AttributeValue().withN("1"))))
    }

    @Test
    fun `item long maybe for existing item key`() {
        val item = mapOf("key" to AttributeValue().withN("1600848379"))
        val value = DynamoAttributes.itemLongValueOrNull(item, "key")
        assertThat(value, equalTo(1600848379L))
    }

    @Test
    fun `item long maybe for non existing item key`() {
        val item = mapOf("key" to AttributeValue())
        val value = DynamoAttributes.itemLongValueOrNull(item, "key")
        assertThat(value, absent())
    }
}
