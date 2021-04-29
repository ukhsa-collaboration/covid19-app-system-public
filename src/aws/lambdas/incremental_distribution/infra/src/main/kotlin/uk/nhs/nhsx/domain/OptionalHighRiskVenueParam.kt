package uk.nhs.nhsx.domain

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class OptionalHighRiskVenueParam(val type: String): StringValue(type) {
    companion object : NonBlankStringValueFactory<OptionalHighRiskVenueParam>(::OptionalHighRiskVenueParam)
}
