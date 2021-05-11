package uk.nhs.nhsx.sanity.stores

import uk.nhs.nhsx.sanity.BaseSanityCheck
import uk.nhs.nhsx.sanity.stores.config.DeployedStore

abstract class S3SanityCheck : BaseSanityCheck() {

    companion object {
        fun stores() = DeployedStore.values()
            .map { env.configFor(it, it.storeReference) }
    }
}
