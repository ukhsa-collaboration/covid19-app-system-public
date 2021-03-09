package uk.nhs.nhsx.virology.tokengen

import java.io.File

data class CtaTokensZip(private val name: String, val content: File) {
    val filename: String
        get() = if (name.endsWith(".zip")) name else "$name.zip"
}
