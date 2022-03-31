@file:Suppress("HttpUrlsUsage")

package uk.nhs.nhsx.crashreports

data class CrashReportRequest(
    val exception: String,
    val threadName: String,
    val stackTrace: String
)

fun CrashReportRequest.sanitiseUrls() = copy(
    exception = exception.sanitiseUrls(),
    threadName = threadName.sanitiseUrls(),
    stackTrace = stackTrace.sanitiseUrls()
)

private fun String.sanitiseUrls() = listOf("http://", "https://")
    .fold(this) { acc, prefix -> acc.replace(prefix, "") }
