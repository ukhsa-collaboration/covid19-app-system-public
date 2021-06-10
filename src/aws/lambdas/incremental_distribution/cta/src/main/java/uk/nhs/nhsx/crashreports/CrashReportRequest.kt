package uk.nhs.nhsx.crashreports

data class CrashReportRequest(
    val exception: String,
    val threadName: String,
    val stackTrace: String
)
