package uk.nhs.nhsx.core

import java.util.UUID

typealias UniqueId = () -> UUID

val RandomUUID: UniqueId = UUID::randomUUID
