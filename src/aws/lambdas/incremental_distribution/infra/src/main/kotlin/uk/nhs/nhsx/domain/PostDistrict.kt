package uk.nhs.nhsx.domain

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.length

class PostDistrict private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<PostDistrict>(::PostDistrict, (1..20).length.let { v -> { v(it.trim()) } })
}
