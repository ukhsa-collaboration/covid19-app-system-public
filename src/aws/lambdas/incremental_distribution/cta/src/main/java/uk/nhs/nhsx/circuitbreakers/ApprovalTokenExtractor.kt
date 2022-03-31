package uk.nhs.nhsx.circuitbreakers

object ApprovalTokenExtractor {

    operator fun invoke(path: String?) = path
        .orEmpty()
        .substringAfterLast("/", missingDelimiterValue = "")
        .takeIf { it.matches("^[a-zA-Z\\d]{50}$".toRegex()) }
}
