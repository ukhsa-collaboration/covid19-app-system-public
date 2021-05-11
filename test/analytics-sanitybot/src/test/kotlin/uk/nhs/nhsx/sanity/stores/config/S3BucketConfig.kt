package uk.nhs.nhsx.sanity.stores.config

import com.fasterxml.jackson.databind.JsonNode
import dev.forkhandles.result4k.get
import dev.forkhandles.result4k.mapFailure
import dev.forkhandles.result4k.resultFrom

sealed class S3BucketConfig(val name: String) {
    override fun toString() = name
}

class CTAStore(storeReference: String) : S3BucketConfig(storeReference) {
    companion object {
        fun from(store: Store) = { config: JsonNode ->
            CTAStore(
                config.requiredText(store.name)
            )
        }
    }
}

class SIPStore(storeReference: String) : S3BucketConfig(storeReference) {
    companion object {
        fun from(store: Store) = { config: JsonNode ->
            SIPStore(
                config.requiredText(store.name)
            )
        }
    }
}

private fun JsonNode.requiredText(name: String): String = resultFrom { this[name]!!.textValue() }
    .mapFailure { "Missing value $name".also { System.err.println(it) } }
    .get()
