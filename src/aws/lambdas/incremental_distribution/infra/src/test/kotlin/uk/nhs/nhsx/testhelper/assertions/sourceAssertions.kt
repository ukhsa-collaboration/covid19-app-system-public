package uk.nhs.nhsx.testhelper.assertions

import strikt.api.Assertion
import uk.nhs.nhsx.core.aws.s3.ByteArraySource

val Assertion.Builder<ByteArraySource>.bytes get() = get(ByteArraySource::bytes)
fun Assertion.Builder<ByteArraySource>.asString() = get(ByteArraySource::toUtf8String)
