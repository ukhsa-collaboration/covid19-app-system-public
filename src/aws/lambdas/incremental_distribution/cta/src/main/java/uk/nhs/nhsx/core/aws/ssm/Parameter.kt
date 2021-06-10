package uk.nhs.nhsx.core.aws.ssm

fun interface Parameter<T> {
    fun value(): T
}
