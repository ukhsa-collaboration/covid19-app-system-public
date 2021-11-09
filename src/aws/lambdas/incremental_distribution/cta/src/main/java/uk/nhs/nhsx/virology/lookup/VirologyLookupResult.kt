package uk.nhs.nhsx.virology.lookup

import uk.nhs.nhsx.core.HttpResponses
import uk.nhs.nhsx.core.Json
import uk.nhs.nhsx.virology.lookup.VirologyLookupResult.AvailableV1
import uk.nhs.nhsx.virology.lookup.VirologyLookupResult.AvailableV2
import uk.nhs.nhsx.virology.lookup.VirologyLookupResult.NotFound
import uk.nhs.nhsx.virology.lookup.VirologyLookupResult.Pending

sealed class VirologyLookupResult {
    class AvailableV1(val response: VirologyLookupResponseV1) : VirologyLookupResult()

    class AvailableV2(val response: VirologyLookupResponseV2) : VirologyLookupResult()

    object Pending : VirologyLookupResult()

    object NotFound : VirologyLookupResult()
}

fun VirologyLookupResult.toHttpResponse() = when (this) {
    is AvailableV1 -> HttpResponses.ok(Json.toJson(response))
    is AvailableV2 -> HttpResponses.ok(Json.toJson(response))
    NotFound -> HttpResponses.notFound("Test result lookup submitted for unknown testResultPollingToken")
    Pending -> HttpResponses.noContent()
}
