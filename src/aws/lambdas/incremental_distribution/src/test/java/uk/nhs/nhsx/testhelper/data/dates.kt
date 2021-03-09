package uk.nhs.nhsx.testhelper.data

import java.time.Instant

fun String.asInstant() = Instant.parse(this)
