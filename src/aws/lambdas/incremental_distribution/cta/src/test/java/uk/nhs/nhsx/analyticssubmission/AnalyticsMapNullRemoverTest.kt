package uk.nhs.nhsx.analyticssubmission

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import uk.nhs.nhsx.analyticssubmission.AnalyticsMapNullRemover.removeNullValues

class AnalyticsMapNullRemoverTest {

    @Test
    fun `removes null values from flattened map`() {
        val input = mapOf(
            "name" to "John",
            "age" to null,
            "addressStreet" to "123 st",
            "addressNumber" to "1",
            "jobName" to "job 12",
            "jobStreet" to "234 st",
            "jobStreetNumber" to null
        )
        val result = removeNullValues(input)
        expectThat(result).isEqualTo(
            mapOf(
                "name" to "John",
                "addressStreet" to "123 st",
                "addressNumber" to "1",
                "jobName" to "job 12",
                "jobStreet" to "234 st"
            )
        )
    }
}
