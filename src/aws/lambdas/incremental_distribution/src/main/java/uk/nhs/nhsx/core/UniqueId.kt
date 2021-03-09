package uk.nhs.nhsx.core

import java.util.UUID
import java.util.function.Supplier

interface UniqueId {
    companion object {
        val ID = Supplier { UUID.randomUUID() }
    }
}
