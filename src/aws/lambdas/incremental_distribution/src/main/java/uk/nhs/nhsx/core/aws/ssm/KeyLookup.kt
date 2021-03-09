package uk.nhs.nhsx.core.aws.ssm

import uk.nhs.nhsx.core.signature.KeyId

fun interface KeyLookup {
    fun kmsKeyId(): KeyId
}
