package uk.nhs.nhsx.core.handler

object RequestContext {
    private val REQUEST_ID = ThreadLocal<String>()
    fun assignAwsRequestId(requestId: String?) = REQUEST_ID.set(requestId)
    fun awsRequestId() = REQUEST_ID.get() ?: "unknown"
}
