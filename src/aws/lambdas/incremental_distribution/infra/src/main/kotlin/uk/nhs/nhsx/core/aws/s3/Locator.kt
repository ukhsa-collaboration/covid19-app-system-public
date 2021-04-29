package uk.nhs.nhsx.core.aws.s3

data class Locator private constructor(val bucket: BucketName, val key: ObjectKey) {
    companion object {
        fun of(name: BucketName, key: ObjectKey) = Locator(name, key)
    }
}
