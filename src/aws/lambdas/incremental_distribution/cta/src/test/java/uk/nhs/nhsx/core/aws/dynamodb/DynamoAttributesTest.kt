package uk.nhs.nhsx.core.aws.dynamodb

import com.amazonaws.services.dynamodbv2.model.AttributeValue
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isEqualTo
import strikt.assertions.isNull

class DynamoAttributesTest {

    @Test
    fun `item value required for existing item key`() {
        val item = mapOf("key" to AttributeValue("value"))
        val value = DynamoAttributes.itemValueOrThrow(item, "key")

        expectThat(value).isEqualTo("value")
    }

    @Test
    fun `item value required for non existing item key`() {
        val item = mapOf("key" to AttributeValue("value"))

        expectThrows<IllegalStateException> {
            DynamoAttributes.itemValueOrThrow(item, "key-123")
        }
    }

    @Test
    fun `item value required for existing item key but with different type`() {
        val item = mapOf("key" to AttributeValue().withN("1"))

        expectThrows<IllegalStateException> {
            DynamoAttributes.itemValueOrThrow(item, "key")
        }
    }

    @Test
    fun `item string maybe for existing item key`() {
        val item = mapOf("key" to AttributeValue("value"))
        val value = DynamoAttributes.itemValueOrNull(item, "key")

        expectThat(value).isEqualTo("value")
    }

    @Test
    fun `item string maybe for non existing item key`() {
        val item = mapOf("key" to AttributeValue("value"))
        val value = DynamoAttributes.itemValueOrNull(item, "key-123")

        expectThat(value).isNull()
    }

    @Test
    fun `item string maybe for existing item key but with different type`() {
        val item = mapOf("key" to AttributeValue().withN("1"))
        val value = DynamoAttributes.itemValueOrNull(item, "key")

        expectThat(value).isNull()
    }

    @Test
    fun `creates singleton map with string attribute value`() {
        val attributeMap = DynamoAttributes.attributeMap("key", "value")

        expectThat(attributeMap).isEqualTo(mapOf("key" to AttributeValue("value")))
    }

    @Test
    fun `creates singleton map with number attribute value`() {
        val attributeMap = DynamoAttributes.attributeMap("key", 1)

        expectThat(attributeMap).isEqualTo(mapOf("key" to AttributeValue().withN("1")))
    }

    @Test
    fun `item long maybe for existing item key`() {
        val item = mapOf("key" to AttributeValue().withN("1600848379"))
        val value = DynamoAttributes.itemLongValueOrNull(item, "key")

        expectThat(value).isEqualTo(1600848379L)
    }

    @Test
    fun `item long maybe for non existing item key`() {
        val item = mapOf("key" to AttributeValue())
        val value = DynamoAttributes.itemLongValueOrNull(item, "key")

        expectThat(value).isNull()
    }
}
