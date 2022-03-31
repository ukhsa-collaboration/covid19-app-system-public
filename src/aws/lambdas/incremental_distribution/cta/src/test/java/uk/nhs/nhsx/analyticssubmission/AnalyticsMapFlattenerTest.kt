package uk.nhs.nhsx.analyticssubmission

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import uk.nhs.nhsx.analyticssubmission.AnalyticsMapFlattener.flattenRecursively

class AnalyticsMapFlattenerTest {

    @Test
    fun `flattens with nested maps`() {
        val input = mapOf(
            "name" to "John",
            "age" to null,
            "address" to mapOf(
                "addressStreet" to "123 st",
                "addressNumber" to "1"
            ),
            "job" to mapOf(
                "jobName" to "job 12",
                "additionalInfo" to mapOf(
                    "address" to mapOf(
                        "jobStreet" to "234 st",
                        "jobStreetNumber" to null
                    )
                )
            )
        )
        val result = flattenRecursively(input)
        expectThat(result).isEqualTo(
            mapOf(
                "name" to "John",
                "age" to null,
                "addressStreet" to "123 st",
                "addressNumber" to "1",
                "jobName" to "job 12",
                "jobStreet" to "234 st",
                "jobStreetNumber" to null
            )
        )
    }

    @Test
    fun `same if no nested values`() {
        val input = mapOf(
            "name" to "John",
            "age" to null
        )
        val result = flattenRecursively(input)
        expectThat(result).isEqualTo(
            mapOf(
                "name" to "John",
                "age" to null
            )
        )
    }

    @Test
    fun `empty when map is already empty`() {
        val input = mapOf<String, Any>()
        val result = flattenRecursively(input)
        expectThat(result).isEqualTo(mapOf())
    }

    private data class Person(val name: String, val address: Address)
    private data class Address(val street: String)

    @Test
    fun `handles different types as input`() {
        val person = Person("John", Address("street 123"))
        val result = flattenRecursively(person)
        expectThat(result).isEqualTo(
            mapOf(
                "name" to "John",
                "street" to "street 123"
            )
        )
    }
}
