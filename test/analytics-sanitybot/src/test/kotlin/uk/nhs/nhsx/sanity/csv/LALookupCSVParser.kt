package uk.nhs.nhsx.sanity.csv

import dev.forkhandles.result4k.peekFailure
import dev.forkhandles.result4k.recover
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import java.io.StringReader

class LALookupCSVParser() {
    fun parse(csv: String) = CSVParser(
        StringReader(csv),
        CSVFormat.RFC4180
            .builder()
            .setIgnoreHeaderCase(true)
            .setIgnoreSurroundingSpaces(true)
            .setIgnoreEmptyLines(true)
            .setHeader()
            .setNullString("")
            .build()
    ).use { p ->
        p.records.map {
            it.asLALookupCSVRecord()
                .peekFailure { error("Failed with parsing local authorities lookup csv file!") }
                .recover { null }
        }
    }
}
