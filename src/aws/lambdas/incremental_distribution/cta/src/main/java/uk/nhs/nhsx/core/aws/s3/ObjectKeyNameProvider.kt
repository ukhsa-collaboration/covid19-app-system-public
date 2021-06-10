package uk.nhs.nhsx.core.aws.s3

fun interface ObjectKeyNameProvider {
    fun generateObjectKeyName(): ObjectKey
}
