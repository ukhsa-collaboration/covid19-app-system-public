package uk.nhs.nhsx.core.signature

import java.time.Instant

data class SignatureDate(
    val string: String,
    val instant: Instant
)
