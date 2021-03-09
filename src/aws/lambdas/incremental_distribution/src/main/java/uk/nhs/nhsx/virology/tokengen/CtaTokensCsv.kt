package uk.nhs.nhsx.virology.tokengen

data class CtaTokensCsv(private val name: String, val content: String) {
    val filename: String
        get() = if (name.endsWith(".csv")) name else "$name.csv"
}
