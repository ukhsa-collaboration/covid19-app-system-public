package uk.nhs.nhsx.core

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class ContentType(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<ContentType>(::ContentType) {
        val APPLICATION_JSON = ContentType.of("application/json")
        val APPLICATION_ZIP = ContentType.of("application/zip")
        val TEXT_PLAIN = ContentType.of("text/plain")
        val TEXT_CSV = ContentType.of("text/csv")
    }
}
