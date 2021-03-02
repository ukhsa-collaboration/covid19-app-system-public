package uk.nhs.nhsx.core.aws.s3

data class MetaHeader(val key: String, val value: String) {
    fun asHttpHeaderName(): String = "X-Amz-Meta-$key"
    fun asS3MetaName(): String = key
}
