package uk.nhs.nhsx.core.aws.dynamodb

import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import java.util.*
import java.util.Collections.singletonMap

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
        val value = DynamoAttributes.itemValueMaybe(item, "key")
        assertThat(value, equalTo(Optional.of("value")))
    }

    @Test
    fun `item string maybe for non existing item key`() {
        val item = mapOf("key" to AttributeValue("value"))
        val value = DynamoAttributes.itemValueMaybe(item, "key-123")
        assertThat(value, equalTo(Optional.empty()))
    }

    @Test
    fun `item string maybe for existing item key but with different type`() {
        val item = mapOf("key" to AttributeValue().withN("1"))
        val value = DynamoAttributes.itemValueMaybe(item, "key")
        assertThat(value, equalTo(Optional.empty()))
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
}