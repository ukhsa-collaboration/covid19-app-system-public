package uk.nhs.nhsx.sanity.csv

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.io.File

class LALookupCSVParserTest {

    private val csvFile = File("../../src/static/analytics-local-authorities-demographic-geographic-lookup-20220125.csv")
    private val csvContent = csvFile.readText()

    @Test
    fun `can parse`() {
        val records = LALookupCSVParser().parse(csvContent)

        expectThat(records.count()).isEqualTo(336)
    }
}
