package uk.nhs.nhsx.domain

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import uk.nhs.nhsx.core.random.crockford.CrockfordDammRandomStringGenerator

class CtaToken private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<CtaToken>(
        ::CtaToken,
        validation = CrockfordDammRandomStringGenerator.checksum()::validate
    )
}
