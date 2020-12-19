package uk.nhs.nhsx.sanity

import org.http4k.aws.AwsSdkClient
import org.http4k.client.JavaHttpClient
import org.http4k.core.then
import org.http4k.filter.DebuggingFilters.PrintRequestAndResponse

open class AwsSanityCheck : BaseSanityCheck() {
    val http = AwsSdkClient(PrintRequestAndResponse(System.err, debugStream = true).then(JavaHttpClient()))
}
