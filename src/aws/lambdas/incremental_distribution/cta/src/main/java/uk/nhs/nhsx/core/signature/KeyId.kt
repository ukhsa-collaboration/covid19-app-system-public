package uk.nhs.nhsx.core.signature

import dev.forkhandles.values.StringValue

class KeyId private constructor(value: String) : StringValue(value) {
    companion object {
        fun of(idOrArn: String) = KeyId(kmsKeyId(idOrArn))

        private fun kmsKeyId(kmsKeyArnOrId: String) = if (kmsKeyArnOrId.startsWith("arn:aws:kms:")) {
            kmsKeyArnOrId.substring(kmsKeyArnOrId.indexOf(":key/") + ":key/".length)
        } else kmsKeyArnOrId
    }
}
