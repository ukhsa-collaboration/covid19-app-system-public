package uk.nhs.nhsx.virology.tokengen

abstract class CtaProcessorResult {
    abstract fun toResponse(): Map<String, String>

    class Success(val zipFilename: String, private val message: String) : CtaProcessorResult() {
        override fun toResponse() = mapOf(
            "result" to "success",
            "message" to message,
            "filename" to zipFilename
        )
    }

    class Error(val message: String) : CtaProcessorResult() {
        override fun toResponse() = mapOf(
            "result" to "error",
            "message" to message
        )
    }
}
